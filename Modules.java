package com.bedro;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** All Bedro modules. Intended for singleplayer, anarchy servers and private servers that allow client mods. */
public final class Modules {
    public static final List<Module> ALL = new ArrayList<>();

    public static Module FLY, SPEED, NOFALL, KILLAURA, AURA, REACH, AUTOCRIT, AUTOMACE,
            XRAY, FULLBRIGHT, ESP, AUTOMINE, AUTOTOTEM, SCAFFOLD;

    private Modules() {}

    public static void init() {
        FLY = add(new Fly());
        SPEED = add(new Speed());
        NOFALL = add(new NoFall());
        KILLAURA = add(new KillAura());
        AURA = add(new Aura());
        REACH = add(new Reach());
        AUTOCRIT = add(new AutoCrit());
        AUTOMACE = add(new AutoMace());
        XRAY = add(new XRay());
        FULLBRIGHT = add(new Fullbright());
        ESP = add(new Esp());
        AUTOMINE = add(new AutoMine());
        AUTOTOTEM = add(new AutoTotem());
        SCAFFOLD = add(new Scaffold());
    }

    private static Module add(Module m) {
        ALL.add(m);
        return m;
    }

    // ---------- helpers ----------

    static Minecraft mc() { return Minecraft.getInstance(); }

    static boolean moving() {
        var o = mc().options;
        return o.keyUp.isDown() || o.keyDown.isDown() || o.keyLeft.isDown() || o.keyRight.isDown();
    }

    static LivingEntity nearestTarget(double range) {
        Player self = mc().player;
        LivingEntity best = null;
        double bestDist = range * range;
        AABB box = self.getBoundingBox().inflate(range);
        for (Entity e : mc().level.getEntities(self, box, x -> x instanceof LivingEntity)) {
            LivingEntity le = (LivingEntity) e;
            if (!le.isAlive() || le.isInvulnerable()) continue;
            double d = self.distanceToSqr(le);
            if (d < bestDist) {
                bestDist = d;
                best = le;
            }
        }
        return best;
    }

    static void attack(Entity target) {
        mc().gameMode.attack(mc().player, target);
        mc().player.swing(InteractionHand.MAIN_HAND);
    }

    static boolean cooldownReady() {
        return mc().player.getAttackStrengthScale(0.5f) >= 0.95f;
    }

    static int findHotbar(java.util.function.Predicate<ItemStack> p) {
        for (int i = 0; i < 9; i++) {
            if (p.test(mc().player.getInventory().getItem(i))) return i;
        }
        return -1;
    }

    public static boolean isXrayBlock(Block b) {
        if (b == Blocks.ANCIENT_DEBRIS || b == Blocks.CHEST || b == Blocks.TRAPPED_CHEST
                || b == Blocks.ENDER_CHEST || b == Blocks.SPAWNER || b == Blocks.LAVA
                || b == Blocks.NETHER_PORTAL || b == Blocks.END_PORTAL_FRAME) return true;
        String path = BuiltInRegistries.BLOCK.getKey(b).getPath();
        return path.endsWith("_ore") || path.equals("ancient_debris") || path.endsWith("shulker_box");
    }

    // ---------- movement ----------

    static class Fly extends Module {
        Fly() { super("Fly"); }
        @Override public void onTick() {
            var a = mc.player.getAbilities();
            a.mayfly = true;
            a.flying = true;
            a.setFlyingSpeed(0.08f);
        }
        @Override protected void onDisable() {
            if (mc.player == null) return;
            var a = mc.player.getAbilities();
            if (!mc.player.isCreative() && !mc.player.isSpectator()) {
                a.mayfly = false;
                a.flying = false;
            }
            a.setFlyingSpeed(0.05f);
        }
    }

    static class Speed extends Module {
        Speed() { super("Speed"); }
        @Override public void onTick() {
            if (!moving() || mc.player.isShiftKeyDown()) return;
            double yaw = Math.toRadians(mc.player.getYRot());
            var o = mc.options;
            double f = (o.keyUp.isDown() ? 1 : 0) - (o.keyDown.isDown() ? 1 : 0);
            double s = (o.keyLeft.isDown() ? 1 : 0) - (o.keyRight.isDown() ? 1 : 0);
            double speed = 0.30;
            double x = (-Math.sin(yaw) * f + Math.cos(yaw) * s);
            double z = (Math.cos(yaw) * f + Math.sin(yaw) * s);
            double len = Math.max(1.0, Math.sqrt(x * x + z * z));
            Vec3 d = mc.player.getDeltaMovement();
            mc.player.setDeltaMovement(x / len * speed, d.y, z / len * speed);
        }
    }

    static class NoFall extends Module {
        NoFall() { super("NoFall"); }
        @Override public void onTick() {
            if (mc.player.fallDistance > 2.0f && mc.getConnection() != null) {
                mc.getConnection().send(new ServerboundMovePlayerPacket.StatusOnly(true, false));
                mc.player.fallDistance = 0;
            }
        }
    }

    // ---------- combat ----------

    static class KillAura extends Module {
        KillAura() { super("KillAura"); }
        @Override public void onTick() {
            LivingEntity t = nearestTarget(4.2);
            if (t == null || !cooldownReady()) return;
            if (AUTOCRIT.enabled && (mc.player.onGround() || mc.player.fallDistance <= 0.0f)) return;
            attack(t);
        }
    }

    static class Aura extends Module {
        Aura() { super("Aura"); }
        @Override public void onTick() {
            if (!cooldownReady()) return;
            AABB box = mc.player.getBoundingBox().inflate(4.2);
            boolean hit = false;
            for (Entity e : mc.level.getEntities(mc.player, box, x -> x instanceof LivingEntity)) {
                if (!e.isAlive() || mc.player.distanceToSqr(e) > 4.2 * 4.2) continue;
                mc.gameMode.attack(mc.player, e);
                hit = true;
            }
            if (hit) mc.player.swing(InteractionHand.MAIN_HAND);
        }
    }

    static class Reach extends Module {
        private static final Identifier ID = Identifier.fromNamespaceAndPath("bedro", "reach");
        Reach() { super("Reach"); }
        private void apply(boolean on) {
            if (mc.player == null) return;
            set(mc.player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE), on);
            set(mc.player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE), on);
        }
        private void set(AttributeInstance inst, boolean on) {
            if (inst == null) return;
            inst.removeModifier(ID);
            if (on) inst.addTransientModifier(new AttributeModifier(ID, 3.0, AttributeModifier.Operation.ADD_VALUE));
        }
        @Override protected void onEnable() { apply(true); }
        @Override protected void onDisable() { apply(false); }
        @Override public void onTick() {
            var inst = mc.player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE);
            if (inst != null && !inst.hasModifier(ID)) apply(true);
        }
    }

    /** Flag module: KillAura waits for a falling (critical) hit while this is enabled. */
    static class AutoCrit extends Module {
        AutoCrit() { super("AutoCrit"); }
    }

    static class AutoMace extends Module {
        AutoMace() { super("AutoMace"); }
        @Override public void onTick() {
            LivingEntity t = nearestTarget(4.2);
            if (t == null) return;
            int slot = findHotbar(s -> s.is(Items.MACE));
            if (slot < 0) return;
            if (mc.player.getInventory().getSelectedSlot() != slot) {
                mc.player.getInventory().setSelectedSlot(slot);
                return;
            }
            // Smash attack needs a real fall; hit once falling fast enough.
            if (mc.player.fallDistance >= 1.5f && !mc.player.onGround() && cooldownReady()) {
                attack(t);
            }
        }
    }

    // ---------- render ----------

    static class XRay extends Module {
        XRay() { super("X-Ray"); }
        @Override protected void onEnable() { mc.levelRenderer.allChanged(); }
        @Override protected void onDisable() { mc.levelRenderer.allChanged(); }
    }

    static class Fullbright extends Module {
        Fullbright() { super("Fullbright"); }
        @Override public void onTick() {
            var eff = mc.player.getEffect(MobEffects.NIGHT_VISION);
            if (eff == null || eff.getDuration() < 400) {
                mc.player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 1_000_000, 0, false, false));
            }
        }
        @Override protected void onDisable() {
            if (mc.player != null) mc.player.removeEffect(MobEffects.NIGHT_VISION);
        }
    }

    static class Esp extends Module {
        Esp() { super("ESP"); }
        @Override public void onTick() {
            for (Entity e : mc.level.entitiesForRendering()) {
                if (e != mc.player && e instanceof LivingEntity) e.setGlowingTag(true);
            }
        }
        @Override protected void onDisable() {
            if (mc.level == null) return;
            for (Entity e : mc.level.entitiesForRendering()) e.setGlowingTag(false);
        }
    }

    // ---------- utility ----------

    static class AutoMine extends Module {
        AutoMine() { super("AutoMine"); }
        @Override public void onTick() {
            boolean looking = mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK;
            mc.options.keyAttack.setDown(looking);
        }
        @Override protected void onDisable() { mc.options.keyAttack.setDown(false); }
    }

    static class AutoTotem extends Module {
        AutoTotem() { super("AutoTotem"); }
        @Override public void onTick() {
            if (mc.screen != null || mc.gameMode == null) return;
            if (mc.player.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) return;
            for (int i = 9; i <= 44; i++) {
                if (mc.player.inventoryMenu.getSlot(i).getItem().is(Items.TOTEM_OF_UNDYING)) {
                    mc.gameMode.handleInventoryMouseClick(mc.player.inventoryMenu.containerId, i, 40, ClickType.SWAP, mc.player);
                    return;
                }
            }
        }
    }

    static class Scaffold extends Module {
        Scaffold() { super("Scaffold"); }
        @Override public void onTick() {
            BlockPos below = mc.player.blockPosition().below();
            if (!mc.level.getBlockState(below).canBeReplaced()) return;
            int slot = findHotbar(s -> s.getItem() instanceof BlockItem);
            if (slot < 0) return;
            mc.player.getInventory().setSelectedSlot(slot);
            for (Direction d : Direction.values()) {
                BlockPos nb = below.relative(d);
                if (mc.level.getBlockState(nb).canBeReplaced()) continue;
                Direction face = d.getOpposite();
                Vec3 hit = Vec3.atCenterOf(nb).add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
                mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(hit, face, nb, false));
                mc.player.swing(InteractionHand.MAIN_HAND);
                return;
            }
        }
    }
}
