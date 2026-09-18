package com.xulai.dnfenhance.enhance;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public final class EnhanceConfig {
    private EnhanceConfig() {}

    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.ConfigValue<List<? extends Double>> SUCCESS_RATES;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> FAILURE_PENALTIES;
    public static final ModConfigSpec.ConfigValue<List<? extends Integer>> CARBON_COSTS;
    public static final ModConfigSpec.ConfigValue<List<? extends Double>> BONUS_PER_LEVELS;
    public static final ModConfigSpec.DoubleValue LUCK_BONUS;
    public static final ModConfigSpec.DoubleValue ADVANCED_BONUS;
    public static final ModConfigSpec.DoubleValue MOB_GEAR_CHANCE;
    public static final ModConfigSpec.DoubleValue CARBON_DROP_CHANCE;
    public static final ModConfigSpec.BooleanValue PROTECTION_CONSUME;

    public static final ModConfigSpec.BooleanValue ENHANCE_MOD_ATTRIBUTES;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> ENHANCE_ATTRIBUTE_BLACKLIST;

    public static final int PIG_MAX_COUNT = 1;

    public static final ModConfigSpec.BooleanValue PIG_ENABLED;
    public static final ModConfigSpec.DoubleValue PIG_SUMMON_CHANCE;
    public static final ModConfigSpec.DoubleValue PIG_BLACKENED_CHANCE;
    public static final ModConfigSpec.DoubleValue PIG_MASTER_CHANCE;
    public static final ModConfigSpec.DoubleValue PIG_LUCK_PER_SUCCESS;
    public static final ModConfigSpec.DoubleValue PIG_AURA_RADIUS;
    public static final ModConfigSpec.DoubleValue PIG_AURA_PENALTY;
    public static final ModConfigSpec.BooleanValue PIG_AURA_STACKING;
    public static final ModConfigSpec.IntValue PIG_BLACKENED_HEALTH;
    public static final ModConfigSpec.IntValue PIG_MASTER_HEALTH;
    public static final ModConfigSpec.IntValue PIG_ADVANCED_CARBON_MIN;
    public static final ModConfigSpec.IntValue PIG_ADVANCED_CARBON_MAX;
    public static final ModConfigSpec.IntValue PIG_CHARM_MIN;
    public static final ModConfigSpec.IntValue PIG_CHARM_MAX;
    public static final ModConfigSpec.IntValue PIG_BLACKENED_CARBON_MIN;
    public static final ModConfigSpec.IntValue PIG_BLACKENED_CARBON_MAX;
    public static final ModConfigSpec.IntValue PIG_BLACKENED_CHARM_MIN;
    public static final ModConfigSpec.IntValue PIG_BLACKENED_CHARM_MAX;
    public static final ModConfigSpec.IntValue PIG_MASTER_CARBON_MIN;
    public static final ModConfigSpec.IntValue PIG_MASTER_CARBON_MAX;
    public static final ModConfigSpec.IntValue PIG_MASTER_CHARM_MIN;
    public static final ModConfigSpec.IntValue PIG_MASTER_CHARM_MAX;
    public static final ModConfigSpec.DoubleValue PIG_MASTER_SONIC_DAMAGE;
    public static final ModConfigSpec.IntValue PIG_MASTER_COMBO_COOLDOWN;
    public static final ModConfigSpec.IntValue PIG_MASTER_TNT_INTERVAL;
    public static final ModConfigSpec.DoubleValue PIG_MASTER_PROXIMITY_RADIUS;
    public static final ModConfigSpec.IntValue PIG_MASTER_PROXIMITY_COOLDOWN;
    public static final ModConfigSpec.IntValue PIG_MASTER_SQUAD_RESPAWN;
    public static final ModConfigSpec.DoubleValue PIG_MASTER_TNT_RANGE;
    public static final ModConfigSpec.IntValue PIG_MASTER_MINION_MIN;
    public static final ModConfigSpec.IntValue PIG_MASTER_MINION_MAX;
    public static final ModConfigSpec.IntValue PIG_MASTER_MINION_LEVEL_MIN;
    public static final ModConfigSpec.IntValue PIG_MASTER_MINION_LEVEL_MAX;
    public static final ModConfigSpec.IntValue PIG_LOOTING_BONUS;

    public static final ModConfigSpec.ConfigValue<List<? extends Double>> TICKET_SUCCESS_RATES;
    public static final ModConfigSpec.ConfigValue<List<? extends Double>> PIG_MASTER_TICKET_RATES;
    public static final ModConfigSpec.ConfigValue<List<? extends Double>> PIG_BLACKENED_TICKET_RATES;
    public static final ModConfigSpec.IntValue PIG_MASTER_TICKET_COUNT;
    public static final ModConfigSpec.IntValue PIG_BLACKENED_TICKET_COUNT;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment(
                "DNF 强化系统全局配置（config/dnfenhance.toml，跨存档共用，修改文件后自动热重载）。",
                "DNF enhancement configuration (COMMON, shared across worlds, hot-reloaded on file change).",
                "下方列表的第 N 项对应 “从 +(N-1) 强化到 +N” 的一次操作；列表长度即强化上限（默认 15 = +15 封顶）。",
                "List entry N applies to the attempt from +(N-1) to +N; the list length defines the max enhance level (default 15).");

        SUCCESS_RATES = builder
                .comment("每一级目标的成功率（0.0~1.0）。第 1 项 = 0→+1，第 15 项 = +14→+15。",
                        "Success rate (0.0-1.0) for each target level. Entry 1 = 0 -> +1, entry 15 = +14 -> +15.")
                .defineList("successRates",
                        List.of(1.0, 1.0, 1.0, 0.90, 0.85, 0.80, 0.70, 0.60, 0.50, 0.40, 0.30, 0.25, 0.20, 0.15, 0.10),
                        o -> o instanceof Number n && n.doubleValue() >= 0.0 && n.doubleValue() <= 1.0);

        builder.comment(" ");

        FAILURE_PENALTIES = builder
                .comment("强化失败时的惩罚：NONE = 无惩罚；DOWN_n = 掉 n 级（如 DOWN_3）；DESTROY = 装备损毁。",
                        "Penalty when an attempt fails: NONE = nothing; DOWN_n = drop n levels (e.g. DOWN_3); DESTROY = item destroyed.",
                        "默认采用 DNF 经典规则：+4~+7 失败掉 1 级，+8~+10 失败掉 3 级，+11 起失败装备损毁。",
                        "Default follows classic DNF rules: +4~+7 drop 1, +8~+10 drop 3, +11 and above destroys the item.")
                .defineList("failurePenalties",
                        List.of("NONE", "NONE", "NONE",
                                "DOWN_1", "DOWN_1", "DOWN_1", "DOWN_1",
                                "DOWN_3", "DOWN_3", "DOWN_3",
                                "DESTROY", "DESTROY", "DESTROY", "DESTROY", "DESTROY"),
                        o -> o instanceof String s && s.matches("(?i)^(NONE|DOWN_\\d+|DESTROY)$"));

        builder.comment(" ");

        CARBON_COSTS = builder
                .comment("每一级目标消耗的炉岩碳数量。",
                        "Furnace Carbon cost for each target level.")
                .defineList("carbonCosts",
                        List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 12, 14, 16, 18, 20),
                        o -> o instanceof Number n && n.intValue() >= 0 && n.intValue() <= 640);

        builder.comment(" ");

        BONUS_PER_LEVELS = builder
                .comment("每级强化的属性成长曲线（对装备上所有被强化的数值生效，弓弩的箭矢伤害也走这里）。",
                        "Stat bonus curve per enhance level (applies to every enhanced value on the item; bow/crossbow arrow damage uses it too).",
                        "第 N 项 = +N 级的加成比例；默认 +1 = 2%，+15 = 100%（数值翻倍）。",
                        "Entry N = bonus ratio at +N; defaults: +1 = 2%, +15 = 100% (values doubled).")
                .defineList("bonusPerLevels",
                        List.of(0.02, 0.03, 0.05, 0.06, 0.08, 0.10, 0.15, 0.20, 0.25, 0.32, 0.41, 0.52, 0.65, 0.81, 1.00),
                        o -> o instanceof Number n && n.doubleValue() >= 0.0 && n.doubleValue() <= 10.0);

        builder.comment(" ");

        LUCK_BONUS = builder
                .comment("强化者处于幸运效果（幸运药水）时的额外成功率。0.10 = +10%。",
                        "Extra success rate while the enhancer has the Luck effect (luck potion). 0.10 = +10%.")
                .defineInRange("luckBonus", 0.10, 0.0, 1.0);

        builder.comment(" ");

        ADVANCED_BONUS = builder
                .comment("高级炉岩碳额外提供的成功率。0.10 = +10%。",
                        "Extra success rate granted by the advanced furnace carbon. 0.10 = +10%.")
                .defineInRange("advancedBonus", 0.10, 0.0, 1.0);

        builder.comment(" ");

        MOB_GEAR_CHANCE = builder
                .comment("敌对生物生成时获得随机强化武器/装备的概率。0.15 = 15%。",
                        "Chance for a newly spawned hostile mob to carry random enhanced gear. 0.15 = 15%.")
                .defineInRange("mobGearChance", 0.15, 0.0, 1.0);

        builder.comment(" ");

        CARBON_DROP_CHANCE = builder
                .comment("携带强化装备的敌对生物死亡后掉落炉岩碳的基础概率（1~3 个），抢夺附魔每级额外 +5%。",
                        "Base chance for a killed hostile mob that carried enhanced gear to drop 1-3 furnace carbon. Looting adds +5% per level.")
                .defineInRange("carbonDropChance", 0.20, 0.0, 1.0);

        builder.comment(" ");

        PROTECTION_CONSUME = builder
                .comment("保护生效时是否消耗 1 个保护石。",
                        "Whether one protection stone is consumed when the protection triggers.")
                .define("protectionConsume", true);

        builder.comment(" ",
                "############################################################",
                "#  属性强化范围 / Attribute Enhancement Scope",
                "#",
                "#  强化提升装备自身的属性数值：默认读取装备上的全部加成",
                "#  （原版 + 其它 MOD 添加的）并按同一比例一起提升。",
                "#  例：同时带「护甲」与「法术强度」的胸甲，两者都会提升。",
                "#",
                "#  只放大正向加成。原版把武器攻速写成负数配平惩罚",
                "#  （钻石剑 -2.4 / 钻石镐 -2.8 / 钻石斧 -3.0，把攻速基础值 4.0",
                "#  压到 1.6 / 1.2 / 1.0），放大后会把攻速推向 0，因此跳过。",
                "#",
                "#  Enhancement scales every bonus on the item - the vanilla",
                "#  attributes plus any added by other mods - by the same ratio.",
                "#  Only positive bonuses are scaled: vanilla writes weapon attack",
                "#  speed as a negative offset (-2.4 / -2.8 / -3.0, bringing the 4.0",
                "#  base down to 1.6 / 1.2 / 1.0), which would collapse to 0.",
                "############################################################");

        ENHANCE_MOD_ATTRIBUTES = builder
                .comment("是否一并强化其它 MOD 添加的属性（法术强度、法术强化等）。",
                        "true = 原版属性与 MOD 属性都强化；false = 只强化原版属性。",
                        "Whether attributes added by other mods are enhanced too.",
                        "true = vanilla + modded; false = vanilla only.")
                .define("enhanceModAttributes", true);

        builder.comment(" ");

        ENHANCE_ATTRIBUTE_BLACKLIST = builder
                .comment("永不参与强化的属性 ID，默认排除武器攻速（原版的配平惩罚）。",
                        "可写完整注册名 minecraft:generic.attack_speed，也可省略命名空间 generic.attack_speed。",
                        "清空本列表 = 不排除任何属性，装备上所有正向加成都会被强化。",
                        "Attribute ids that never scale; weapon attack speed is excluded by default.",
                        "Full id or bare path both work. Empty list = exclude nothing.")
                .defineListAllowEmpty("enhanceAttributeBlacklist",
                        List.of("minecraft:generic.attack_speed"),
                        o -> o instanceof String s && s.matches("(?i)^([a-z0-9_.-]+:)?[a-z0-9/._-]+$"));

        builder.comment(" ",
                "############################################################",
                "#  凯莉猪家族 / Kai'li Family",
                "#",
                "#  强化时按概率召唤凯莉猪来捣乱，共三种：",
                "#",
                "#   【凯莉】",
                "#     光环内强化成功率下降；击杀后掉落材料。",
                "#",
                "#   【凯莉·黑化】",
                "#     光环内强化必定失败；全套 +10 铁甲、HP 50，掉落更多。",
                "#     被玩家攻击后：投掷 TNT + 召唤强化生物（无音波）。",
                "#",
                "#   【凯莉·强化大师】",
                "#     中立；光环内强化必定失败，且失败后等级归 0；全套 +10 钻石甲、HP 100。",
                "#     受击后：音波 + 朝击退落点投 TNT + 召唤强化生物。",
                "#     每 5 秒朝玩家投 TNT；玩家靠近 2 格内释放音波（10 秒冷却）；",
                "#     小队全灭 10 秒后重新召唤；击杀后掉落强化券。",
                "#",
                "#  每次强化成功都会提高三种凯莉的出现概率，出现任意一头后归零。",
                "#  玩家用命名牌自己命名的凯莉猪只有光环效果（不掉落、无特殊行为）。",
                "#",
                "#  Three flavours, summoned by chance while enhancing:",
                "#",
                "#   Kai'li           aura lowers the success rate; drops materials when killed.",
                "#",
                "#   Darkened Kai'li  aura forces failure; +10 iron gear, 50 HP, more loot.",
                "#                    On hit: TNT + summoned mobs (no sonic boom).",
                "#",
                "#   Enhance Master   neutral; aura forces failure and resets the level to 0;",
                "#                    +10 diamond gear, 100 HP. On hit: sonic boom + TNT at the",
                "#                    knockback point + summoned mobs. Lobs TNT every 5s, sonic",
                "#                    boom when a player gets within 2 blocks (10s cooldown),",
                "#                    re-summons its squad 10s after a wipe, drops enhance tickets.",
                "#",
                "#  Each successful enhancement raises all three chances; resets when one appears.",
                "#  Pigs named by players only get the aura (no loot, no special behaviour).",
                "############################################################");

        PIG_ENABLED = builder
                .comment("是否启用凯莉猪机制。false = 完全关闭（不会召唤，也没有任何光环效果）。",
                        "Enable the Kai'li mechanic. false = fully disabled (no summons, no aura).")
                .define("kaiLiPigEnabled", true);

        builder.comment(" ");

        PIG_SUMMON_CHANCE = builder
                .comment("普通「凯莉」的基础出现概率。1.0 = 100%，0.05 = 5%。",
                        "Base spawn chance of a normal Kai'li. 1.0 = 100%, 0.05 = 5%.")
                .defineInRange("kaiLiSummonChance", 0.05, 0.0, 1.0);

        builder.comment(" ");

        PIG_BLACKENED_CHANCE = builder
                .comment("「凯莉·黑化」的基础出现概率。0.01 = 1%。",
                        "Base spawn chance of the Darkened Kai'li. 0.01 = 1%.")
                .defineInRange("kaiLiBlackenedChance", 0.01, 0.0, 1.0);

        builder.comment(" ");

        PIG_MASTER_CHANCE = builder
                .comment("「凯莉·强化大师」的基础出现概率。0.001 = 0.1%。",
                        "Base spawn chance of the Enhance Master Kai'li. 0.001 = 0.1%.")
                .defineInRange("kaiLiMasterChance", 0.001, 0.0, 1.0);

        builder.comment(" ");

        PIG_LUCK_PER_SUCCESS = builder
                .comment("每次强化成功为三种凯莉增加的出现概率。0.02 = 每次 +2%。",
                        "Spawn chance added to all three Kai'li per successful enhancement. 0.02 = +2% each.")
                .defineInRange("kaiLiLuckPerSuccess", 0.02, 0.0, 1.0);

        builder.comment(" ");

        PIG_AURA_RADIUS = builder
                .comment("凯莉猪的干扰光环半径（格），以强化炉为中心计算。10 = 强化炉 10 格范围内。",
                        "Radius (blocks) of the interference aura around the furnace. 10 = within 10 blocks.")
                .defineInRange("kaiLiAuraRadius", 10.0, 0.0, 128.0);

        builder.comment(" ");

        PIG_AURA_PENALTY = builder
                .comment("光环内每头普通「凯莉」降低的成功率比例。0.5 = 降低 50%（当前成功率 × 0.5）。",
                        "Success-rate penalty per normal Kai'li inside the aura. 0.5 = -50% (current rate x 0.5).")
                .defineInRange("kaiLiAuraPenalty", 0.5, 0.0, 1.0);

        builder.comment(" ");

        PIG_AURA_STACKING = builder
                .comment("光环内有多头普通凯莉时，惩罚是否叠加。",
                        "Whether the penalty stacks when several normal Kai'li are inside the aura.",
                        "true  = 每头都乘一次（2 头 = ×0.25）；false = 只按 1 头计算。",
                        "true = multiply per pig (2 pigs = x0.25); false = count as a single pig.")
                .define("kaiLiAuraStacking", false);

        builder.comment(" ");

        PIG_BLACKENED_HEALTH = builder
                .comment("「凯莉·黑化」的生命值。",
                        "Health of the Darkened Kai'li.")
                .defineInRange("kaiLiBlackenedHealth", 50, 1, 1024);

        builder.comment(" ");

        PIG_MASTER_HEALTH = builder
                .comment("「凯莉·强化大师」的生命值。",
                        "Health of the Enhance Master Kai'li.")
                .defineInRange("kaiLiMasterHealth", 100, 1, 1024);

        builder.comment(" ");

        PIG_ADVANCED_CARBON_MIN = builder
                .comment("击杀普通「凯莉」掉落高级炉岩碳的最少数量。",
                        "Minimum advanced furnace carbon dropped by a killed normal Kai'li.")
                .defineInRange("kaiLiAdvancedCarbonMin", 2, 0, 64);

        builder.comment(" ");

        PIG_ADVANCED_CARBON_MAX = builder
                .comment("击杀普通「凯莉」掉落高级炉岩碳的最多数量。",
                        "Maximum advanced furnace carbon dropped by a killed normal Kai'li.")
                .defineInRange("kaiLiAdvancedCarbonMax", 5, 0, 64);

        builder.comment(" ");

        PIG_CHARM_MIN = builder
                .comment("击杀普通「凯莉」掉落装备保护石的最少数量。",
                        "Minimum protection stones dropped by a killed normal Kai'li.")
                .defineInRange("kaiLiCharmMin", 1, 0, 64);

        builder.comment(" ");

        PIG_CHARM_MAX = builder
                .comment("击杀普通「凯莉」掉落装备保护石的最多数量。",
                        "Maximum protection stones dropped by a killed normal Kai'li.")
                .defineInRange("kaiLiCharmMax", 2, 0, 64);

        builder.comment(" ");

        PIG_BLACKENED_CARBON_MIN = builder
                .comment("击杀「凯莉·黑化」掉落高级炉岩碳的最少数量。",
                        "Minimum advanced furnace carbon dropped by a killed Darkened Kai'li.")
                .defineInRange("kaiLiBlackenedCarbonMin", 4, 0, 64);

        builder.comment(" ");

        PIG_BLACKENED_CARBON_MAX = builder
                .comment("击杀「凯莉·黑化」掉落高级炉岩碳的最多数量。",
                        "Maximum advanced furnace carbon dropped by a killed Darkened Kai'li.")
                .defineInRange("kaiLiBlackenedCarbonMax", 32, 0, 64);

        builder.comment(" ");

        PIG_BLACKENED_CHARM_MIN = builder
                .comment("击杀「凯莉·黑化」掉落装备保护石的最少数量。",
                        "Minimum protection stones dropped by a killed Darkened Kai'li.")
                .defineInRange("kaiLiBlackenedCharmMin", 8, 0, 64);

        builder.comment(" ");

        PIG_BLACKENED_CHARM_MAX = builder
                .comment("击杀「凯莉·黑化」掉落装备保护石的最多数量。",
                        "Maximum protection stones dropped by a killed Darkened Kai'li.")
                .defineInRange("kaiLiBlackenedCharmMax", 16, 0, 64);

        builder.comment(" ");

        PIG_MASTER_CARBON_MIN = builder
                .comment("击杀「凯莉·强化大师」掉落高级炉岩碳的最少数量。",
                        "Minimum advanced furnace carbon dropped by a killed Enhance Master.")
                .defineInRange("kaiLiMasterCarbonMin", 32, 0, 64);

        builder.comment(" ");

        PIG_MASTER_CARBON_MAX = builder
                .comment("击杀「凯莉·强化大师」掉落高级炉岩碳的最多数量。",
                        "Maximum advanced furnace carbon dropped by a killed Enhance Master.")
                .defineInRange("kaiLiMasterCarbonMax", 64, 0, 64);

        builder.comment(" ");

        PIG_MASTER_CHARM_MIN = builder
                .comment("击杀「凯莉·强化大师」掉落装备保护石的最少数量。",
                        "Minimum protection stones dropped by a killed Enhance Master.")
                .defineInRange("kaiLiMasterCharmMin", 20, 0, 64);

        builder.comment(" ");

        PIG_MASTER_CHARM_MAX = builder
                .comment("击杀「凯莉·强化大师」掉落装备保护石的最多数量。",
                        "Maximum protection stones dropped by a killed Enhance Master.")
                .defineInRange("kaiLiMasterCharmMax", 30, 0, 64);

        builder.comment(" ");

        PIG_MASTER_SONIC_DAMAGE = builder
                .comment("「凯莉·强化大师」被攻击时音波攻击造成的伤害。10.0 = 原版坚守者音波伤害。",
                        "Damage of the Enhance Master's sonic boom retaliation. 10.0 = vanilla warden value.")
                .defineInRange("kaiLiMasterSonicDamage", 10.0, 0.0, 100.0);

        builder.comment(" ");

        PIG_MASTER_COMBO_COOLDOWN = builder
                .comment("「凯莉·强化大师」受击连招（音波+击退落点TNT+召唤）的冷却时间（tick）。20 = 1 秒。",
                        "Cooldown (ticks) of the Enhance Master's on-hit combo (boom + TNT at knockback point + minions). 20 = 1 second.")
                .defineInRange("kaiLiMasterComboCooldown", 20, 0, 12000);

        builder.comment(" ");

        PIG_MASTER_TNT_INTERVAL = builder
                .comment("「凯莉·强化大师」周期性朝玩家投掷点燃 TNT 的间隔（tick）。100 = 5 秒。",
                        "Interval (ticks) at which the Enhance Master lobs lit TNT at players. 100 = 5 seconds.")
                .defineInRange("kaiLiMasterTntInterval", 100, 20, 12000);

        builder.comment(" ");

        PIG_MASTER_PROXIMITY_RADIUS = builder
                .comment("「凯莉·强化大师」近身音波的触发半径（格）。玩家进入该范围即释放音波。",
                        "Radius (blocks) that triggers the Enhance Master's proximity sonic boom.")
                .defineInRange("kaiLiMasterProximityRadius", 2.0, 0.0, 16.0);

        builder.comment(" ");

        PIG_MASTER_PROXIMITY_COOLDOWN = builder
                .comment("「凯莉·强化大师」近身音波的冷却时间（tick）。200 = 10 秒。",
                        "Cooldown (ticks) of the Enhance Master's proximity sonic boom. 200 = 10 seconds.")
                .defineInRange("kaiLiMasterProximityCooldown", 200, 20, 12000);

        builder.comment(" ");

        PIG_MASTER_SQUAD_RESPAWN = builder
                .comment("「凯莉·强化大师」召唤的小队全部死亡后，再次召唤前的冷却时间（tick）。200 = 10 秒。",
                        "Cooldown (ticks) before the Enhance Master summons a new squad after the previous one is wiped. 200 = 10 seconds.")
                .defineInRange("kaiLiMasterSquadRespawnCooldown", 200, 20, 12000);

        builder.comment(" ");

        PIG_MASTER_TNT_RANGE = builder
                .comment("「凯莉·强化大师」投掷 TNT 的感知半径（格）。16 = 16 格内的攻击者会被砸。",
                        "Range (blocks) at which the Enhance Master lobs TNT at its attacker. 16 = 16 blocks.")
                .defineInRange("kaiLiMasterTntRange", 16.0, 1.0, 64.0);

        builder.comment(" ");

        PIG_MASTER_MINION_MIN = builder
                .comment("「凯莉·强化大师」被攻击时召唤敌对生物的最少数量。",
                        "Minimum hostile mobs summoned by an attacked Enhance Master.")
                .defineInRange("kaiLiMasterMinionMin", 1, 0, 8);

        builder.comment(" ");

        PIG_MASTER_MINION_MAX = builder
                .comment("「凯莉·强化大师」被攻击时召唤敌对生物的最多数量。",
                        "Maximum hostile mobs summoned by an attacked Enhance Master.")
                .defineInRange("kaiLiMasterMinionMax", 3, 0, 8);

        builder.comment(" ");

        PIG_MASTER_MINION_LEVEL_MIN = builder
                .comment("被召唤敌对生物装备的最低强化等级。",
                        "Minimum enhance level of the summoned mobs' gear.")
                .defineInRange("kaiLiMasterMinionLevelMin", 6, 0, 15);

        builder.comment(" ");

        PIG_MASTER_MINION_LEVEL_MAX = builder
                .comment("被召唤敌对生物装备的最高强化等级。",
                        "Maximum enhance level of the summoned mobs' gear.")
                .defineInRange("kaiLiMasterMinionLevelMax", 10, 0, 15);

        builder.comment(" ");

        PIG_LOOTING_BONUS = builder
                .comment("击杀者主手抢夺附魔每级额外增加的掉落数量（对三种凯莉的掉落都生效）。",
                        "Extra drops per Looting level on the killer's weapon (applies to all three variants).")
                .defineInRange("kaiLiLootingBonus", 1, 0, 10);

        builder.comment(" ",
                "############################################################",
                "#  装备强化券 / Enhance Tickets",
                "#  +10~+15 共 6 种，手持需要强化的装备和强化券即可直接强化（消耗 1 张券）。",
                "#  仅由「凯莉·黑化」与「凯莉·强化大师」掉落；9 张同档券可合成 1 张高一级的券。",
                "#",
                "#  Six tickets (+10 to +15): hold the equipment in one hand and the ticket in the",
                "#  other, then use the ticket to enhance straight to the ticket level (consumes 1).",
                "#  Dropped only by Darkened Kai'li and the Enhance Master; combine 9 same-tier tickets",
                "#  into 1 ticket of the next tier.",
                "############################################################");

        TICKET_SUCCESS_RATES = builder
                .comment("强化券的成功率（第 1~6 项 = +10~+15 券）。1.0 = 100% 必定成功。",
                        "Enhance ticket success rates (entries 1-6 = +10 to +15 tickets). 1.0 = always succeeds.")
                .defineList("ticketSuccessRates",
                        List.of(1.0, 1.0, 1.0, 1.0, 1.0, 1.0),
                        o -> o instanceof Number n && n.doubleValue() >= 0.0 && n.doubleValue() <= 1.0);

        builder.comment(" ");

        PIG_MASTER_TICKET_RATES = builder
                .comment("大师掉落强化券时各档位的权重（第 1~6 项 = +10~+15 的相对概率，每张券按权重随机定档）。",
                        "Weights per tier when the Enhance Master drops tickets (entries 1-6 = +10 to +15, relative chance).",
                        "掉落数量见 kaiLiMasterTicketCount。 / Amount: kaiLiMasterTicketCount.")
                .defineList("kaiLiMasterTicketRates",
                        List.of(0.10, 0.20, 0.30, 0.50, 0.50, 0.50),
                        o -> o instanceof Number n && n.doubleValue() >= 0.0 && n.doubleValue() <= 1.0);

        builder.comment(" ");

        PIG_BLACKENED_TICKET_RATES = builder
                .comment("黑化掉落强化券时各档位的权重（第 1~6 项 = +10~+15 的相对概率，每张券按权重随机定档）。",
                        "Weights per tier when the Darkened Kai'li drops tickets (entries 1-6 = +10 to +15, relative chance).",
                        "掉落数量见 kaiLiBlackenedTicketCount。 / Amount: kaiLiBlackenedTicketCount.")
                .defineList("kaiLiBlackenedTicketRates",
                        List.of(0.90, 0.80, 0.70, 0.30, 0.20, 0.10),
                        o -> o instanceof Number n && n.doubleValue() >= 0.0 && n.doubleValue() <= 1.0);

        builder.comment(" ");

        PIG_MASTER_TICKET_COUNT = builder
                .comment("击杀「凯莉·强化大师」掉落的强化券数量（每张的档位按上方权重随机决定）。",
                        "Number of tickets dropped by a killed Enhance Master (each ticket picks its tier by the weights above).")
                .defineInRange("kaiLiMasterTicketCount", 5, 0, 64);

        builder.comment(" ");

        PIG_BLACKENED_TICKET_COUNT = builder
                .comment("击杀「凯莉·黑化」掉落的强化券数量（每张的档位按上方权重随机决定）。",
                        "Number of tickets dropped by a killed Darkened Kai'li (each ticket picks its tier by the weights above).")
                .defineInRange("kaiLiBlackenedTicketCount", 3, 0, 64);

        SPEC = builder.build();
    }
}
