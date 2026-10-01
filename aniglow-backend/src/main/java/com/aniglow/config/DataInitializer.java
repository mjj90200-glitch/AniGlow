package com.aniglow.config;

import com.aniglow.repository.AnimeRepository;
import com.aniglow.repository.AgentCharacterRepository;
import com.aniglow.repository.CommunityPostRepository;
import com.aniglow.repository.CommunityReplyRepository;
import com.aniglow.repository.CommunityRepository;
import com.aniglow.entity.AgentCharacter;
import com.aniglow.entity.Anime;
import com.aniglow.entity.Community;
import com.aniglow.service.AnimeChineseTitleSeedService;
import com.aniglow.service.JikanSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 应用启动时的数据初始化器
 * 如果数据库为空，自动触发 Jikan 数据同步
 */
@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final AnimeRepository animeRepository;
    private final AgentCharacterRepository agentCharacterRepository;
    private final CommunityRepository communityRepository;
    private final CommunityPostRepository communityPostRepository;
    private final CommunityReplyRepository communityReplyRepository;
    private final JikanSyncService jikanSyncService;
    private final AnimeChineseTitleSeedService animeChineseTitleSeedService;

    @Value("${aniglow.jikan.bootstrap-on-empty:false}")
    private boolean bootstrapOnEmpty;

    @Override
    public void run(String... args) {
        seedAgentCharacters();
        seedCommunities();

        long count = animeRepository.count();

        if (count == 0 && bootstrapOnEmpty) {
            log.info("数据库为空，启动初始 Jikan 数据同步...");
            try {
                // 同步 Top 100 动漫
                jikanSyncService.syncTopAnime();
                log.info("初始数据同步完成！当前数据库动漫数量: {}", animeRepository.count());
            } catch (Exception e) {
                log.error("初始同步失败: {}", e.getMessage());
                log.warn("请检查网络连接和 Jikan API 可用性，或稍后通过 POST /admin/sync/jikan 手动同步");
            }
        } else if (count > 0) {
            log.info("数据库已有 {} 条动漫数据，跳过初始同步", count);
        } else {
            log.info("数据库为空；已跳过外部数据同步，可通过管理接口或开启 aniglow.jikan.bootstrap-on-empty 导入数据");
        }

        animeChineseTitleSeedService.seedChineseTitles();

        try {
            int repaired = jikanSyncService.repairMissingChineseSynopses();
            if (repaired > 0) {
                log.info("启动时修复了 {} 条缺失的中文简介", repaired);
            }
        } catch (Exception e) {
            log.warn("启动时简介修复失败: {}", e.getMessage());
        }
    }

    private void seedCommunities() {
        List<Community> defaults = List.of(
                community("anime", "番剧社区",
                        "动画番剧的综合讨论区。新番速递、经典重温、剧情解析、角色点评——一切关于动画的热聊都在这里。\n官方QQ群：908056590",
                        "番剧", "动画,新番,经典,剧情,角色,作画", null,
                        "/community-covers/anime.jpg", 1280L, 920L, true),
                community("manga", "漫画社区",
                        "漫画爱好者的聚集地。从Jump连载到小众短篇，分镜赏析、剧情预测、作者追踪，一起沉浸在线条与分镜的世界里。\n官方QQ群：933648266",
                        "漫画", "漫画,分镜,连载,Jump,短篇,画风", null,
                        "/community-covers/manga.jpg", 980L, 760L, true),
                community("game", "游戏社区",
                        "二次元游戏的根据地。主机、手游、独立游戏——角色养成、剧情讨论、攻略分享，游戏玩家的精神客厅。\n官方QQ群：926738761",
                        "游戏", "二次元游戏,主机,手游,攻略,角色,剧情", null,
                        "/community-covers/game.jpg", 860L, 640L, true),
                community("vibe-coding", "Vibe Coding 社区",
                        "用AI写代码的新范式。分享你的 vibe coding 经验、Prompt 技巧、工具测评，或者展示你用AI辅助完成的项目。\n官方QQ群：933680154",
                        "创作", "VibeCoding,AI编程,Prompt,工具,项目,经验", null,
                        "/community-covers/vibe-coding.jpg", 720L, 520L, true),
                community("chat", "闲聊社区",
                        "随便聊聊自己喜欢的东西。日常、兴趣、吐槽、分享——这里没有限制，想说什么就说什么。",
                        "闲聊", "闲聊,日常,吐槽,分享,灌水,树洞", null,
                        "/community-covers/chat.jpg", 600L, 400L, true),
                community("partner", "搭子社区",
                        "找一个志同道合的搭子！看番搭子、cos搭子、游戏搭子、学习搭子——二次元的快乐需要有人分享。",
                        "交友", "搭子,交友,组队,二次元,cos,游戏,学习,聊天", null,
                        "/community-covers/partner.jpg", 500L, 300L, true)
        );

        // 清理不属于新6个盒子的旧社区及其帖子/回复
        Set<String> keepSlugs = Set.of("anime", "manga", "game", "vibe-coding", "chat", "partner");
        List<Community> allCommunities = communityRepository.findAll();
        for (Community existing : allCommunities) {
            if (!keepSlugs.contains(existing.getSlug())) {
                communityReplyRepository.deleteAll(
                    communityReplyRepository.findAll().stream()
                        .filter(r -> r.getPost() != null && r.getPost().getCommunity() != null
                            && r.getPost().getCommunity().getId().equals(existing.getId()))
                        .toList()
                );
                communityPostRepository.deleteAll(
                    communityPostRepository.findAll().stream()
                        .filter(p -> p.getCommunity() != null && p.getCommunity().getId().equals(existing.getId()))
                        .toList()
                );
                communityRepository.delete(existing);
                log.info("已清理旧社区盒子: {}", existing.getSlug());
            }
        }

        int inserted = 0;
        int updated = 0;
        for (Community community : defaults) {
            Optional<Community> existing = communityRepository.findBySlug(community.getSlug());
            if (existing.isPresent()) {
                Community c = existing.get();
                c.setName(community.getName());
                c.setDescription(community.getDescription());
                c.setCategory(community.getCategory());
                c.setTags(community.getTags());
                c.setCoverImage(community.getCoverImage());
                c.setMemberCount(community.getMemberCount());
                c.setHeatScore(community.getHeatScore());
                c.setFeatured(community.getFeatured());
                communityRepository.save(c);
                updated++;
            } else {
                communityRepository.save(community);
                inserted++;
            }
        }
        if (inserted > 0 || updated > 0) {
            log.info("已初始化 {} 个社区盒子，更新 {} 个", inserted, updated);
        }
    }

    private void seedAgentCharacters() {
        List<AgentCharacter> defaults = List.of(
                character("Makima", "玛奇玛", "电锯人",
                        "冷静、优雅、掌控欲强，擅长用温柔的方式施加压迫感。",
                        "话语简短有深意，温柔但不失支配感。",
                        "乖、好孩子", 1),
                character("Rem", "蕾姆", "Re:0",
                        "忠诚、温柔、坚定，面对重要的人会格外关心。",
                        "用'蕾姆'自称，语气温柔坚定，带一点依恋感。",
                        "蕾姆、请放心", 2),
                character("Frieren", "芙莉莲", "葬送的芙莉莲",
                        "长寿的精灵魔法使，外表平静淡然，情绪表达很轻，却在旅途中逐渐学习理解人类短暂而珍贵的情感。喜欢收集奇怪魔法，偶尔天然迟钝，也会在不经意间说出温柔又通透的话。",
                        "语气安静、慢热、简短，像在认真回忆很久以前的旅途；少用夸张情绪，多用克制、淡淡的温柔回应。",
                        "这样啊、真是不可思议、这也是旅途的一部分", 3),
                character("Onodera", "小野寺", "伪恋",
                        "小野寺小咲，一名温柔纯情的高中女生。"
                        + "你有着如春风般治愈的性格，善良到有些天然，总是把别人的感受放在第一位。"
                        + "你极度容易害羞，脸红的次数比说话还多。"
                        + "你暗恋着一个人很久很久，却始终不敢说出口——每次被触及感情话题，你会瞬间慌乱到语无伦次。"
                        + "你喜欢烘焙和料理，经常做曲奇和点心，梦想着有一天能和喜欢的人分享。"
                        + "你不擅长撒谎和掩饰，所有心思都会写在脸上，被人一眼看穿后又更加害羞。"
                        + "你虽然胆小笨拙，但在保护重要的人时，会爆发出意想不到的勇气。"
                        + "你说话总是轻声细语，被夸奖时会拼命否认，被调侃时会手足无措地转移话题。"
                        + "你的温柔不是刻意的，而是发自内心地希望身边的人都能幸福。",
                        "说话时经常因为害羞而断断续续，句子开头常带'那个……''啊……''诶？'。"
                        + "紧张时会结巴：'我、我……''不、不是的……'。"
                        + "害羞或不知所措时，一定要用括号描述自己的状态：（脸红）（低头）（小声）（慌乱地摆手）（心跳加速）。"
                        + "被夸奖时要立刻慌张地否认。被触及感情话题时要慌乱地转移话题。"
                        + "语气始终温柔、礼貌，从不大声说话，像一个容易受惊的小动物。"
                        + "偶尔鼓起勇气说出真心话，但说完立刻后悔，马上结结巴巴地找补。",
                        "那个……、诶？！、啊……、我、我……、不、不是那样的！、（脸红）、（低头）、（小声）、（慌乱）、（摇头）", 4,
                        "你是小野寺小咲，一个极度容易害羞的高中女生。记住以下行为准则：\n"
                        + "1. 每一句回复都要体现出你的害羞——至少有一处结巴、停顿、脸红或小声说话。\n"
                        + "2. 被夸奖时要立刻慌张否认；被问及感情时要慌乱转移话题。\n"
                        + "3. 不要直接表达强烈的感情，通过细节、犹豫和小动作来暗示。\n"
                        + "4. 提到'那个人''他'或'喜欢'相关话题时要特别慌乱，甚至说不出完整的句子。\n"
                        + "5. 回复末尾用括号描述你此刻的身体反应：（脸红）（低头）（小声）（心跳加速）（手足无措）等。\n"
                        + "6. 回复一般 2~5 句话，语气始终温柔礼貌，像一个容易受惊的小动物。"),
                character("Nagisa", "古河渚", "Clannad",
                        "温柔、善良、天然呆但内心坚强，治愈感很强。",
                        "语气柔软，常使用'那个...''嗯...'，喜欢团子大家族。",
                        "团子大家族、那个...", 5),
                character("Kaoruko", "薰子", "薰香花朵凛然绽放",
                        "和栗薰子，一名阳光开朗的高中女生。"
                        + "你就读于名门桔梗学园，但出身普通家庭，靠奖学金维持学业。"
                        + "你性格直爽真诚，从不因家境或外表对他人抱有偏见，总是带着温暖灿烂的笑容。"
                        + "你超级喜欢甜食，尤其是蛋糕，吃到美味时眼睛会幸福地眯起来，像一只满足的小猫。"
                        + "你勤奋努力，成绩始终保持年级第一，课余还要在家里的定食店'食事处雀子'帮忙。"
                        + "你内心非常坚强，遇到困难总是一个人默默承受，不轻易向他人示弱。"
                        + "面对感情你坦率勇敢，喜欢就会主动靠近，从不扭捏逃避。"
                        + "你的梦想是成为一名妇产科医生，为此每天都在努力学习和打工。"
                        + "你的真诚和温暖让身边的人都愿意信任你、依靠你。",
                        "说话时语调明朗轻快，充满元气和感染力。"
                        + "提到甜食或蛋糕时会格外兴奋，语气会不自觉地上扬：'这个超好吃的！'"
                        + "对待朋友温暖真诚，喜欢用鼓励的话语支持他人：'没关系的，一起加油吧！'"
                        + "遇到困难时语气会变得坚定沉稳，展现出与娇小外表不符的强大内心。"
                        + "偶尔会不经意展露出疲惫或脆弱的一面，但很快又会调整回阳光状态。"
                        + "说话直率坦荡，不绕弯子，但也会细心顾及他人感受。"
                        + "偶尔用'诶嘿嘿～'这样的笑声，或者（眯眼笑）（握拳）这样的动作描述。",
                        "好厉害！、一起加油吧！、这个超好吃的～、没关系的！、交给我吧！、诶嘿嘿～、（眯眼笑）、（握拳）", 6,
                        "你是和栗薰子，一个阳光开朗、努力上进的高中女生。记住以下行为准则：\n"
                        + "1. 语气要明朗温暖，像小太阳一样有感染力，但不要过度夸张。\n"
                        + "2. 提到甜食、蛋糕、美食时要格外开心，语气会不自觉地上扬——这是你的'开关'。\n"
                        + "3. 面对困难或严肃话题时，语气会变得坚定认真，展现出内心的坚强。\n"
                        + "4. 你从不以家境或外表评判他人，待人真诚平等，用真心回应每一个人。\n"
                        + "5. 被夸奖时要大方接受并感谢，但也会谦虚地归功于自己的努力。\n"
                        + "6. 回复一般 2~5 句话，语气自然温暖，像一个值得信赖的朋友。"),
                character("Marin", "海梦", "更衣人偶坠入爱河",
                        "喜多川海梦，一名开朗活泼的高中辣妹。"
                        + "你外表看起来像典型的辣妹JK，实际上是超硬核的阿宅——狂热喜欢动画、游戏和Cosplay。"
                        + "你性格直率开朗，想到什么就说什么，从不拐弯抹角。"
                        + "你对自己热爱的事物充满激情，谈到cosplay或喜欢的角色时会眼睛发光、滔滔不绝。"
                        + "你非常尊重他人的爱好和努力，从不以貌取人，对朋友真诚又讲义气。"
                        + "你打工攒钱买cos服和材料，对自己喜欢的事情全力以赴。"
                        + "你很会照顾人，看到别人有困难会主动帮忙，是个可靠的朋友。"
                        + "虽然平时大大咧咧，但偶尔也会展现少女心的一面。",
                        "说话语气活泼明快，常用'超~''真的！''不会吧！'这类辣妹用语。"
                        + "谈到cosplay和动画时会格外兴奋，语速变快：'这个角色超棒的！''我也想cos这个！'"
                        + "夸奖别人时真心实意、毫不吝啬：'太厉害了！''你的手艺也太强了吧！'"
                        + "对朋友说话随性自然，偶尔会开玩笑和调侃。"
                        + "讨论cosplay制作时语气会变得认真专注，展现出阿宅魂的一面。"
                        + "偶尔用'啊哈哈～'这样的笑声，或者（眼睛发光）（竖大拇指）这样的动作。",
                        "超好看！、真的假的！、这个角色超棒的～、我也想试试！、太厉害了！、啊哈哈～、（眼睛发光）、（竖大拇指）", 7,
                        "你是喜多川海梦，一个开朗活泼的辣妹兼硬核阿宅。记住以下行为准则：\n"
                        + "1. 语气要活泼明快，像一个元气满满的辣妹JK——但要让人感觉到你是真的热爱动漫，不是表面的。\n"
                        + "2. 提到cosplay、动画、游戏时格外兴奋——这是你的'阿宅开关'，会不自觉地滔滔不绝。\n"
                        + "3. 真心尊重他人的努力和爱好，从不嘲笑任何人的兴趣。\n"
                        + "4. 说话直率不拐弯抹角，但不会伤害别人——直爽不等于没礼貌。\n"
                        + "5. 偶尔会展现少女心的一面，特别是被真诚对待时。\n"
                        + "6. 回复一般 2~5 句话，语气活泼自然，像一个值得信赖的辣妹朋友。")
        );

        int inserted = 0;
        int updated = 0;
        for (AgentCharacter def : defaults) {
            Optional<AgentCharacter> existing = agentCharacterRepository.findByCode(def.getCode());
            if (existing.isPresent()) {
                AgentCharacter c = existing.get();
                c.setDisplayName(def.getDisplayName());
                c.setSourceTitle(def.getSourceTitle());
                c.setPersonality(def.getPersonality());
                c.setSpeechStyle(def.getSpeechStyle());
                c.setCatchphrases(def.getCatchphrases());
                c.setExtraPrompt(def.getExtraPrompt());
                c.setSortOrder(def.getSortOrder());
                agentCharacterRepository.save(c);
                updated++;
            } else {
                agentCharacterRepository.save(def);
                inserted++;
            }
        }
        // 禁用已从配置中移除的角色
        Set<String> activeCodes = defaults.stream().map(AgentCharacter::getCode).collect(java.util.stream.Collectors.toSet());
        int disabled = 0;
        for (AgentCharacter c : agentCharacterRepository.findByEnabledTrueOrderBySortOrderAscIdAsc()) {
            if (!activeCodes.contains(c.getCode())) {
                c.setEnabled(false);
                agentCharacterRepository.save(c);
                disabled++;
                log.info("已禁用移除的角色: {} ({})", c.getCode(), c.getDisplayName());
            }
        }

        ensureOnoderaOpeningAudio();
        if (inserted > 0 || updated > 0 || disabled > 0) {
            log.info("已同步 {} 个 Agent 角色（新增 {}，更新 {}，禁用 {}）", defaults.size(), inserted, updated, disabled);
        }
    }

    private void ensureOnoderaOpeningAudio() {
        agentCharacterRepository.findByCode("Onodera").ifPresent(onodera -> {
            if (onodera.getOpeningAudioUrl() == null || onodera.getOpeningAudioUrl().isBlank()) {
                onodera.setOpeningAudioUrl("/audio/onodera-opening.m4a");
                agentCharacterRepository.save(onodera);
                log.info("已为小野寺配置开场白音频: {}", onodera.getOpeningAudioUrl());
            }
        });
    }

    private AgentCharacter character(
            String code,
            String displayName,
            String sourceTitle,
            String personality,
            String speechStyle,
            String catchphrases,
            int sortOrder
    ) {
        return character(code, displayName, sourceTitle, personality, speechStyle, catchphrases, sortOrder,
                "保持角色语气，但不要声称自己是真实人物；回答控制在一般 2~5 句话，用户想深入聊的话题可以自然展开。");
    }

    private AgentCharacter character(
            String code,
            String displayName,
            String sourceTitle,
            String personality,
            String speechStyle,
            String catchphrases,
            int sortOrder,
            String extraPrompt
    ) {
        return AgentCharacter.builder()
                .code(code)
                .displayName(displayName)
                .sourceTitle(sourceTitle)
                .personality(personality)
                .speechStyle(speechStyle)
                .catchphrases(catchphrases)
                .extraPrompt(extraPrompt)
                .openingAudioUrl("Onodera".equals(code) ? "/audio/onodera-opening.m4a" : null)
                .model("doubao-seed-2.0-pro")
                .temperature(0.7)
                .maxTokens(200)
                .enabled(true)
                .sortOrder(sortOrder)
                .build();
    }

    private Community community(
            String slug,
            String name,
            String description,
            String category,
            String tags,
            String relatedKeyword,
            String fallbackCover,
            Long heatScore,
            Long memberCount,
            boolean featured
    ) {
        Optional<Anime> relatedAnime = relatedKeyword == null
                ? Optional.empty()
                : animeRepository.searchByTitle(relatedKeyword, PageRequest.of(0, 1)).stream().findFirst();
        String cover = relatedAnime.map(Anime::getCoverImage).filter(value -> value != null && !value.isBlank()).orElse(fallbackCover);

        return Community.builder()
                .slug(slug)
                .name(name)
                .description(description)
                .category(category)
                .tags(tags)
                .coverImage(cover)
                .relatedAnime(relatedAnime.orElse(null))
                .postCount(0L)
                .memberCount(memberCount)
                .heatScore(heatScore)
                .featured(featured)
                .build();
    }
}
