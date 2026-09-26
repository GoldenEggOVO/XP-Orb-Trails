package dev.goldeneggovo.xporbtrails;

import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.BlendFactor;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.phys.Vec3;
import net.minecraft.resources.Identifier;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import dev.goldeneggovo.xporbtrails.mixin.RenderPipelinesAccessor;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;

public final class TrailRenderer {
    private static final int CURVE_STEPS = 4;
    private static final int TUBE_FACES = 3;
    private static final int MAX_POINTS = 128;
    private static final RenderPipeline ALPHA_PIPELINE = createPipeline("xp_orb_trails_alpha",
            new BlendFunction(BlendFactor.SRC_ALPHA, BlendFactor.ONE_MINUS_SRC_ALPHA));
    private static final RenderPipeline ADDITIVE_PIPELINE = createPipeline("xp_orb_trails_additive",
            new BlendFunction(BlendFactor.SRC_ALPHA, BlendFactor.ONE));
    private static final StagedVertexBuffer BUFFER =
            new StagedVertexBuffer(() -> "XP Orb Trail Buffer", RenderType.SMALL_BUFFER_SIZE);
    private static final Vector4f COLOR_MODULATOR = new Vector4f(1, 1, 1, 1);
    private static final Vector3f MODEL_OFFSET = new Vector3f();
    private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();
    private static final Map<Integer, Trail> TRAILS = new LinkedHashMap<>();
    private static volatile List<RenderTrail> renderTrails = List.of();
    private static ClientLevel lastLevel;

    private TrailRenderer() { }

    public static synchronized void track(ExperienceOrb orb) {
        TrailConfig cfg = XpOrbTrailsClient.CONFIG;
        if (!cfg.enabled || !(orb.level() instanceof ClientLevel level)) return;
        if (lastLevel != level) {
            TRAILS.clear();
            lastLevel = level;
        }

        Vec3 point = trailPoint(orb, 1.0F, cfg);
        if (point == null) return;
        long now = System.nanoTime();
        Trail trail = TRAILS.computeIfAbsent(orb.getId(), ignored -> new Trail());
        trail.lastSeen = now;
        trail.disappearedAt = 0L;
        trail.append(point, now, cfg.pointSpacing);
        trail.flow(cfg.smoothFlow);

        while (TRAILS.size() > cfg.trailCap) {
            Iterator<Integer> iterator = TRAILS.keySet().iterator();
            iterator.next();
            iterator.remove();
        }
    }

    private static Vec3 trailPoint(ExperienceOrb orb, float partialTick, TrailConfig cfg) {
        Minecraft client = Minecraft.getInstance();
        Vec3 source = orb.getPosition(partialTick).add(0, cfg.orbYOffset, 0);
        Vec3 point = source;
        Vec3 motion = orb.getDeltaMovement();
        double speed = motion.length();
        if (cfg.motionShift > 0.0 && speed > 0.045) {
            double t = clamp((speed - 0.045) / 0.075);
            t = t * t * (3.0 - 2.0 * t);
            point = point.subtract(motion.scale(cfg.motionShift * t / speed));
        }
        if (cfg.cameraPush > 0.0 && client.gameRenderer != null) {
            Vec3 away = point.subtract(client.gameRenderer.mainCamera().position());
            double length = away.length();
            if (length > 1.0E-6) point = point.add(away.scale(cfg.cameraPush / length));
        }
        return point;
    }

    public static synchronized void extract(LevelExtractionContext context) {
        long now = System.nanoTime();
        TrailConfig cfg = XpOrbTrailsClient.CONFIG;
        long lifetime = (long) (cfg.lifetimeSeconds * 1_000_000_000L);
        Vec3 camera = context.camera().position();
        double rangeSq = cfg.renderRange * cfg.renderRange;
        List<RenderTrail> snapshot = new ArrayList<>();

        float partialTick = Math.max(0.0F, Math.min(1.0F,
                context.deltaTracker().getGameTimeDeltaPartialTick(true)));
        Iterator<Map.Entry<Integer, Trail>> iterator = TRAILS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, Trail> entry = iterator.next();
            Trail trail = entry.getValue();
            trail.points.removeIf(point -> now - point.time > lifetime);
            ExperienceOrb liveOrb = context.level().getEntity(entry.getKey()) instanceof ExperienceOrb orb
                    && orb.isAlive() && !orb.isRemoved() ? orb : null;
            Vec3 livePoint = liveOrb == null ? null : trailPoint(liveOrb, partialTick, cfg);
            if (liveOrb != null) {
                trail.disappearedAt = 0L;
                trail.pickupConfirmed = false;
            } else if (trail.disappearedAt == 0L) {
                trail.disappearedAt = now;
                Vec3 lastPoint = trail.points.isEmpty() ? null : trail.points.get(trail.points.size() - 1).position;
                trail.pickupConfirmed = lastPoint != null && Minecraft.getInstance().player != null
                        && Minecraft.getInstance().player.getPosition(1.0F).distanceToSqr(lastPoint) <= 9.0;
            }
            long fadeNow = trail.disappearedAt == 0L ? now : trail.disappearedAt;
            double disappearanceFade = trail.disappearedAt == 0L ? 1.0
                    : 1.0 - smoothStep((now - trail.disappearedAt)
                    / (cfg.pickupFadeSeconds * 1_000_000_000.0));
            double flashProgress = trail.disappearedAt == 0L || !trail.pickupConfirmed ? -1.0
                    : (now - trail.disappearedAt) / (cfg.pickupFlashSeconds * 1_000_000_000.0);
            boolean flashVisible = cfg.pickupFlash && flashProgress >= 0.0 && flashProgress < 1.0;
            if ((trail.points.isEmpty() && now - trail.lastSeen > lifetime)
                    || (disappearanceFade <= 0.001 && !flashVisible)) {
                iterator.remove();
                continue;
            }
            if (trail.points.size() >= 2 && trail.points.get(trail.points.size() - 1).position.distanceToSqr(camera) <= rangeSq) {
                snapshot.add(new RenderTrail(sampleCurve(trail.points, livePoint), fadeNow, disappearanceFade, flashProgress));
            }
        }
        renderTrails = List.copyOf(snapshot);
    }

    public static void render(LevelRenderContext context) {
        List<RenderTrail> snapshot = renderTrails;
        if (snapshot.isEmpty()) return;

        RenderPipeline pipeline = XpOrbTrailsClient.CONFIG.additiveGlow ? ADDITIVE_PIPELINE : ALPHA_PIPELINE;
        VertexFormat format = pipeline.getVertexFormatBinding(0);
        if (format == null) return;
        PrimitiveTopology topology = pipeline.getPrimitiveTopology();
        StagedVertexBuffer.Draw draw = BUFFER.appendDraw(format, topology);
        VertexConsumer vertices = BUFFER.getVertexBuilder(draw);
        Vec3 camera = context.levelState().cameraRenderState.pos;
        TrailConfig cfg = XpOrbTrailsClient.CONFIG;
        long lifetime = (long) (cfg.lifetimeSeconds * 1_000_000_000L);

        for (RenderTrail trail : snapshot) {
            appendTube(vertices, trail.samples, camera, trail.now, lifetime,
                    cfg.width, cfg.opacity * cfg.glowStrength * trail.opacity,
                    cfg.startColor, cfg.endColor, cfg.colorMode, cfg.rainbowSpeed,
                    cfg.tailWidthScale, cfg.middleWidthScale, cfg.headWidthScale);
            if (cfg.pickupFlash && trail.flashProgress >= 0.0 && trail.flashProgress < 1.0 && !trail.samples.isEmpty()) {
                appendPickupFlash(vertices, trail.samples.get(trail.samples.size() - 1).position, camera,
                        trail.flashProgress, cfg.width, cfg.opacity * cfg.glowStrength * cfg.pickupFlashStrength,
                        cfg.pickupFlashSize, cfg.pickupFlashStyle, cfg.endColor, cfg.colorMode, cfg.rainbowSpeed, trail.now);
            }
        }

        BUFFER.upload();
        StagedVertexBuffer.ExecuteInfo info = BUFFER.getExecuteInfo(draw);
        if (info != null) draw(info, pipeline);
        BUFFER.endFrame();
    }

    private static void appendTube(VertexConsumer out, List<Sample> samples, Vec3 camera,
                                   long now, long lifetime, double width, double opacity,
                                   int startColor, int endColor, String colorMode,
                                   double rainbowSpeed, double tailScale, double middleScale, double headScale) {
        if (samples.size() < 2) return;
        Vec3[][] rings = new Vec3[samples.size()][TUBE_FACES];
        int[] colors = new int[samples.size()];
        Frame[] frames = trailFrames(samples);

        for (int i = 0; i < samples.size(); i++) {
            Sample sample = samples.get(i);
            double progress = i / (double) (samples.size() - 1);
            double age = clamp((now - sample.time) / (double) lifetime);
            double ageFade = Math.pow(1.0 - smoothStep(age), 0.72);
            double headFade = smoothStep(Math.min(1.0, progress * 1.25));
            double widthScale;
            if (progress < 0.5) {
                double t = smoothStep(progress * 2.0);
                widthScale = tailScale + (middleScale - tailScale) * t;
            } else {
                double t = smoothStep((progress - 0.5) * 2.0);
                widthScale = middleScale + (headScale - middleScale) * t;
            }
            double radius = width * 0.5 * widthScale;
            int rgb;
            if ("solid".equals(colorMode)) {
                rgb = startColor;
            } else if ("rainbow".equals(colorMode)) {
                double seconds = now / 1_000_000_000.0;
                rgb = hsvToRgb((float) ((progress * 0.72 + seconds * rainbowSpeed) % 1.0), 0.88F, 1.0F);
            } else {
                rgb = mixColor(startColor, endColor, (float) progress);
            }
            int alpha = (int) (255.0 * opacity * ageFade * (0.18 + 0.82 * headFade));
            colors[i] = (Math.max(0, Math.min(255, alpha)) << 24) | rgb;

            for (int face = 0; face < TUBE_FACES; face++) {
                double angle = Math.PI * 2.0 * face / TUBE_FACES;
                rings[i][face] = sample.position
                        .add(frames[i].normal.scale(Math.cos(angle) * radius))
                        .add(frames[i].binormal.scale(Math.sin(angle) * radius))
                        .subtract(camera);
            }
        }

        for (int i = 0; i < rings.length - 1; i++) {
            for (int face = 0; face < TUBE_FACES; face++) {
                int next = (face + 1) % TUBE_FACES;
                vertex(out, rings[i][face], colors[i]);
                vertex(out, rings[i + 1][face], colors[i + 1]);
                vertex(out, rings[i + 1][next], colors[i + 1]);
                vertex(out, rings[i][next], colors[i]);
            }
        }
    }

    private static void vertex(VertexConsumer out, Vec3 p, int color) {
        out.addVertex((float) p.x, (float) p.y, (float) p.z).setColor(color);
    }

    private static void appendPickupFlash(VertexConsumer out, Vec3 center, Vec3 camera,
                                          double progress, double width, double opacity,
                                          double size, String style, int endColor, String colorMode, double rainbowSpeed, long now) {
        double eased = smoothStep(progress);
        double radius = width * size * (0.65 + 2.0 * eased);
        double fade = 1.0 - smoothStep(progress);
        int rgb = "rainbow".equals(colorMode)
                ? hsvToRgb((float) ((now / 1_000_000_000.0 * rainbowSpeed + 0.72) % 1.0), 0.75F, 1.0F)
                : endColor;
        int alpha = Math.max(0, Math.min(255, (int) (255.0 * opacity * fade)));
        int color = (alpha << 24) | rgb;
        Vec3 c = center.subtract(camera);
        if ("ring".equals(style)) {
            flashRing(out, center, camera, radius, color);
        } else if ("star".equals(style)) {
            double thin = radius * 0.16;
            flashDiamond(out, c, new Vec3(radius, 0, 0), new Vec3(0, thin, 0), color);
            flashDiamond(out, c, new Vec3(0, radius, 0), new Vec3(0, 0, thin), color);
            flashDiamond(out, c, new Vec3(0, 0, radius), new Vec3(thin, 0, 0), color);
        } else {
            flashDiamond(out, c, new Vec3(radius, 0, 0), new Vec3(0, radius, 0), color);
            flashDiamond(out, c, new Vec3(radius, 0, 0), new Vec3(0, 0, radius), color);
            flashDiamond(out, c, new Vec3(0, radius, 0), new Vec3(0, 0, radius), color);
        }
    }

    private static void flashDiamond(VertexConsumer out, Vec3 center, Vec3 a, Vec3 b, int color) {
        vertex(out, center.add(a), color);
        vertex(out, center.add(b), color);
        vertex(out, center.subtract(a), color);
        vertex(out, center.subtract(b), color);
    }

    private static void flashRing(VertexConsumer out, Vec3 center, Vec3 camera, double radius, int color) {
        Vec3 view = normalized(camera.subtract(center), new Vec3(0, 0, 1));
        Vec3 axis = Math.abs(view.y) < 0.92 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
        Vec3 right = normalized(view.cross(axis), new Vec3(1, 0, 0));
        Vec3 up = normalized(right.cross(view), new Vec3(0, 1, 0));
        Vec3 c = center.subtract(camera);
        double inner = radius * 0.68;
        int segments = 16;
        for (int i = 0; i < segments; i++) {
            double a0 = Math.PI * 2.0 * i / segments;
            double a1 = Math.PI * 2.0 * (i + 1) / segments;
            Vec3 d0 = right.scale(Math.cos(a0)).add(up.scale(Math.sin(a0)));
            Vec3 d1 = right.scale(Math.cos(a1)).add(up.scale(Math.sin(a1)));
            vertex(out, c.add(d0.scale(inner)), color);
            vertex(out, c.add(d0.scale(radius)), color);
            vertex(out, c.add(d1.scale(radius)), color);
            vertex(out, c.add(d1.scale(inner)), color);
        }
    }

    private static Vec3 tangent(List<Sample> samples, int index) {
        Vec3 a = samples.get(Math.max(0, index - 1)).position;
        Vec3 b = samples.get(Math.min(samples.size() - 1, index + 1)).position;
        Vec3 result = b.subtract(a);
        return result.lengthSqr() < 1.0E-8 ? new Vec3(0, 1, 0) : result.normalize();
    }

    private static Frame[] trailFrames(List<Sample> samples) {
        Frame[] result = new Frame[samples.size()];
        Vec3 tangent = tangent(samples, 0);
        Vec3 normal = initialNormal(tangent);
        Vec3 binormal = normalized(tangent.cross(normal), new Vec3(0, 0, 1));
        result[0] = new Frame(normal, binormal);
        for (int i = 1; i < samples.size(); i++) {
            tangent = tangent(samples, i);
            Vec3 transported = normal.subtract(tangent.scale(normal.dot(tangent)));
            normal = normalized(transported, initialNormal(tangent));
            binormal = normalized(tangent.cross(normal), binormal);
            result[i] = new Frame(normal, binormal);
        }
        return result;
    }

    private static Vec3 initialNormal(Vec3 tangent) {
        Vec3 axis = Math.abs(tangent.y) < 0.92 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
        return normalized(tangent.cross(axis), new Vec3(1, 0, 0));
    }

    private static Vec3 normalized(Vec3 value, Vec3 fallback) {
        double length = value.length();
        return length <= 1.0E-6 ? fallback : value.scale(1.0 / length);
    }

    private static double clamp(double value) { return Math.max(0.0, Math.min(1.0, value)); }
    private static double smoothStep(double value) { value = clamp(value); return value * value * (3.0 - 2.0 * value); }

    private static List<Sample> sampleCurve(List<Point> originalPoints, Vec3 livePoint) {
        List<Point> points = originalPoints;
        if (livePoint != null && !originalPoints.isEmpty()) {
            points = new ArrayList<>(originalPoints);
            Point last = points.get(points.size() - 1);
            points.set(points.size() - 1, new Point(livePoint, last.time));
        }
        List<Sample> result = new ArrayList<>();
        for (int i = 0; i < points.size() - 1; i++) {
            Point p0 = points.get(Math.max(0, i - 1));
            Point p1 = points.get(i);
            Point p2 = points.get(i + 1);
            Point p3 = points.get(Math.min(points.size() - 1, i + 2));
            for (int step = 0; step < CURVE_STEPS; step++) {
                double t = step / (double) CURVE_STEPS;
                result.add(new Sample(catmull(p0.position, p1.position, p2.position, p3.position, t),
                        (long) (p1.time + (p2.time - p1.time) * t)));
            }
        }
        Point last = points.get(points.size() - 1);
        result.add(new Sample(last.position, last.time));
        return result;
    }

    private static Vec3 catmull(Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, double t) {
        return new Vec3(catmull(p0.x, p1.x, p2.x, p3.x, t),
                catmull(p0.y, p1.y, p2.y, p3.y, t),
                catmull(p0.z, p1.z, p2.z, p3.z, t));
    }

    private static double catmull(double p0, double p1, double p2, double p3, double t) {
        double t2 = t * t;
        double t3 = t2 * t;
        return 0.5 * ((2 * p1) + (-p0 + p2) * t
                + (2 * p0 - 5 * p1 + 4 * p2 - p3) * t2
                + (-p0 + 3 * p1 - 3 * p2 + p3) * t3);
    }

    private static int mixColor(int a, int b, float t) {
        int r = (int) (((a >> 16) & 255) + (((b >> 16) & 255) - ((a >> 16) & 255)) * t);
        int g = (int) (((a >> 8) & 255) + (((b >> 8) & 255) - ((a >> 8) & 255)) * t);
        int bl = (int) ((a & 255) + ((b & 255) - (a & 255)) * t);
        return (r << 16) | (g << 8) | bl;
    }

    private static int hsvToRgb(float h, float s, float v) {
        float sector = (h - (float) Math.floor(h)) * 6.0F;
        int i = (int) sector;
        float f = sector - i;
        float p = v * (1 - s), q = v * (1 - s * f), t = v * (1 - s * (1 - f));
        float r, g, b;
        switch (i % 6) {
            case 0 -> { r = v; g = t; b = p; }
            case 1 -> { r = q; g = v; b = p; }
            case 2 -> { r = p; g = v; b = t; }
            case 3 -> { r = p; g = q; b = v; }
            case 4 -> { r = t; g = p; b = v; }
            default -> { r = v; g = p; b = q; }
        }
        return (Math.round(r * 255) << 16) | (Math.round(g * 255) << 8) | Math.round(b * 255);
    }

    private static void draw(StagedVertexBuffer.ExecuteInfo info, RenderPipeline pipeline) {
        Minecraft client = Minecraft.getInstance();
        GpuBufferSlice transforms = RenderSystem.getDynamicUniforms().writeTransform(
                RenderSystem.getModelViewMatrixCopy(), COLOR_MODULATOR, MODEL_OFFSET, TEXTURE_MATRIX);
        RenderTarget target = client.gameRenderer.mainRenderTarget();
        GpuTextureView color = target.getColorTextureView();
        if (color == null) return;

        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                () -> "XP Orb Trails", color, Optional.empty(), target.getDepthTextureView(), OptionalDouble.empty())) {
            pass.setPipeline(RenderSystem.getCompiledPipeline(pipeline));
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("DynamicTransforms", transforms);
            pass.setVertexBuffer(0, info.vertexBuffer().slice());
            pass.setIndexBuffer(info.indexBuffer(), info.indexType());
            pass.drawIndexed(info.indexCount(), 1, info.firstIndex(), info.baseVertex(), 0);
        }
    }

    private static RenderPipeline createPipeline(String name, BlendFunction blend) {
        return RenderPipelinesAccessor.xporbtrails$register(RenderPipeline.builder()
                .withLocation(Identifier.fromNamespaceAndPath("xporbtrails", name))
                .withBindGroupLayout(BindGroupLayouts.PROJECTION)
                .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
                .withVertexShader(Identifier.withDefaultNamespace("core/position_color"))
                .withFragmentShader(Identifier.withDefaultNamespace("core/position_color"))
                .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
                .withPrimitiveTopology(PrimitiveTopology.QUADS)
                .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
                .withCull(false)
                .withColorTargetState(new ColorTargetState(blend))
                .build());
    }

    public static synchronized void close() {
        BUFFER.close();
        TRAILS.clear();
        renderTrails = List.of();
    }

    private static final class Trail {
        final List<Point> points = new ArrayList<>();
        long lastSeen;
        long disappearedAt;
        boolean pickupConfirmed;

        void append(Vec3 position, long now, double spacing) {
            if (points.isEmpty()) {
                points.add(new Point(position, now));
                return;
            }
            Point previous = points.get(points.size() - 1);
            double distance = previous.position.distanceTo(position);
            if (points.size() > 1) {
                Point before = points.get(points.size() - 2);
                if (before.position.distanceTo(position) < spacing) {
                    points.set(points.size() - 1, new Point(position, now));
                    return;
                }
            }
            if (distance < 0.006) return;
            int inserts = Math.min(8, Math.max(1, (int) Math.ceil(distance / spacing)));
            for (int i = 1; i <= inserts; i++) {
                double t = i / (double) inserts;
                points.add(new Point(previous.position.lerp(position, t),
                        (long) (previous.time + (now - previous.time) * t)));
            }
            while (points.size() > MAX_POINTS) points.remove(0);
        }

        void flow(double strength) {
            for (int i = 0; i < points.size() - 1; i++) {
                Point point = points.get(i);
                Point next = points.get(i + 1);
                double trailFactor = 1.0 - i / (double) Math.max(1, points.size() - 1);
                double amount = (0.008 + strength * 0.055) * (0.35 + trailFactor * 0.65);
                points.set(i, new Point(point.position.lerp(next.position, amount), point.time));
            }
        }
    }

    private record Point(Vec3 position, long time) { }
    private record Sample(Vec3 position, long time) { }
    private record RenderTrail(List<Sample> samples, long now, double opacity, double flashProgress) { }
    private record Frame(Vec3 normal, Vec3 binormal) { }
}
