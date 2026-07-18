package com.aniglow.service;

import com.aniglow.entity.Anime;
import com.aniglow.repository.AnimeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static java.util.Map.entry;

/**
 * 中文搜索名补全。
 *
 * 页面展示仍使用原始番名，这里只为搜索提供中文译名、简称和常见别名。
 * 后续如果接入 Bangumi/自维护 CSV，可以继续写入 titleCn/searchAliases 两个字段。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnimeChineseTitleSeedService {

    private final AnimeRepository animeRepository;

    private static final Map<Long, ChineseTitle> CHINESE_TITLES = Map.ofEntries(
            entry(52991L, cn("葬送的芙莉莲", "芙莉莲,葬送芙莉莲")),
            entry(61469L, cn("JOJO的奇妙冒险 飙马野郎", "飙马野郎,钢球快跑,SBR,JOJO第七部")),
            entry(5114L, cn("钢之炼金术师FA", "钢炼,钢之炼金术师 Brotherhood,钢之炼金术师兄弟会")),
            entry(57555L, cn("电锯人 剧场版 蕾塞篇", "链锯人,电锯人,蕾塞篇")),
            entry(9253L, cn("命运石之门", "石头门,Steins Gate")),
            entry(38524L, cn("进击的巨人 第三季 Part.2", "巨人,进击巨人,进击的巨人第三季")),
            entry(28977L, cn("银魂 第三季", "银魂°")),
            entry(39486L, cn("银魂 THE FINAL", "银魂最终篇,银魂完结篇")),
            entry(11061L, cn("全职猎人 2011", "猎人,猎人2011,Hunter Hunter")),
            entry(60022L, cn("海贼王 粉丝来信", "航海王粉丝来信,One Piece粉丝来信")),
            entry(9969L, cn("银魂 第二季", "银魂'")),
            entry(15417L, cn("银魂 延长战", "银魂延长篇")),
            entry(820L, cn("银河英雄传说", "银英传")),
            entry(34096L, cn("银魂 第四季", "银魂。")),
            entry(41467L, cn("死神 千年血战篇", "境界千年血战篇,BLEACH千年血战篇")),
            entry(43608L, cn("辉夜大小姐想让我告白 第三季", "辉夜大小姐,辉夜告白,辉夜大小姐想让我告白Ultra Romantic")),
            entry(42938L, cn("水果篮子 最终季", "水果篮子Final")),
            entry(4181L, cn("CLANNAD After Story", "团子大家族,CLANNAD第二季")),
            entry(918L, cn("银魂", "银他妈")),
            entry(28851L, cn("声之形", "电影声之形")),
            entry(2904L, cn("反叛的鲁路修R2", "叛逆的鲁鲁修R2,Code Geass R2")),
            entry(59978L, cn("葬送的芙莉莲 第二季", "芙莉莲第二季,葬送芙莉莲第二季")),
            entry(58514L, cn("药屋少女的呢喃 第二季", "药屋的独语第二季,药师少女的独语第二季")),
            entry(61316L, cn("Re:从零开始的异世界生活 第四季", "从零开始第四季,Re0第四季,蕾姆")),
            entry(35180L, cn("三月的狮子 第二季", "3月的狮子第二季")),
            entry(15335L, cn("银魂剧场版 完结篇 万事屋永远", "银魂完结篇,万事屋永远")),
            entry(19L, cn("怪物", "Monster")),
            entry(37491L, cn("银魂 银之魂篇 后半战", "银之魂篇后半")),
            entry(51535L, cn("进击的巨人 最终季 完结篇", "巨人完结篇,进击的巨人完结篇")),
            entry(35247L, cn("终物语 第二季", "终物语下")),
            entry(54492L, cn("药屋少女的呢喃", "药屋的独语,药师少女的独语,猫猫")),
            entry(40682L, cn("王者天下 第三季", "王国第三季,Kingdom第三季")),
            entry(59571L, cn("进击的巨人 完结篇 THE LAST ATTACK", "巨人剧场版,进击的巨人最后的进击")),
            entry(37987L, cn("紫罗兰永恒花园 剧场版", "薇尔莉特剧场版,京紫剧场版")),
            entry(49387L, cn("冰海战记 第二季", "海盗战记第二季,Vinland Saga第二季")),
            entry(32281L, cn("你的名字。", "你的名字,君名")),
            entry(2921L, cn("明日之丈2", "明日之丈第二季")),
            entry(36838L, cn("银魂 银之魂篇", "银之魂篇")),
            entry(40028L, cn("进击的巨人 最终季", "巨人最终季")),
            entry(58788L, cn("异国日记", "违国日记")),
            entry(37510L, cn("灵能百分百 第二季", "路人超能100第二季,灵能100第二季")),
            entry(31758L, cn("伤物语 III 冷血篇", "伤物语冷血篇")),
            entry(37521L, cn("冰海战记", "海盗战记,Vinland Saga")),
            entry(263L, cn("第一神拳", "一步神拳")),
            entry(32935L, cn("排球少年 乌野高中 VS 白鸟泽学园高中", "排球少年第三季,排球少年白鸟泽")),
            entry(199L, cn("千与千寻", "千与千寻的神隐")),
            entry(48583L, cn("进击的巨人 最终季 Part.2", "巨人最终季第二部分")),
            entry(17074L, cn("物语系列 第二季", "物语系列Second Season")),
            entry(60489L, cn("章鱼哔的原罪", "章鱼噼的原罪,章鱼P的原罪")),
            entry(1L, cn("星际牛仔", "赏金猎人,牛仔比波普")),
            entry(39894L, cn("吹响吧！上低音号 第三季", "吹响上低音号第三季,京吹第三季")),
            entry(47917L, cn("孤独摇滚！", "波奇酱,波奇摇滚,孤独摇滚")),
            entry(50160L, cn("王者天下 第四季", "王国第四季,Kingdom第四季")),
            entry(21L, cn("海贼王", "航海王,One Piece")),
            entry(60058L, cn("我推的孩子 第三季", "推子第三季")),
            entry(55016L, cn("偶像", "我推的孩子OP,Idol")),
            entry(53223L, cn("王者天下 第五季", "王国第五季,Kingdom第五季")),
            entry(52215L, cn("地。关于地球的运动", "地球的运动,Chi")),
            entry(52198L, cn("辉夜大小姐想让我告白 初吻不会结束", "辉夜大小姐初吻,辉夜剧场版")),
            entry(24701L, cn("虫师 续章 第二季", "虫师续章后半")),
            entry(45649L, cn("灌篮高手 THE FIRST SLAM DUNK", "灌篮高手电影,灌篮高手")),
            entry(50172L, cn("灵能百分百 第三季", "路人超能100第三季,灵能100第三季")),
            entry(48569L, cn("86 不存在的战区 Part.2", "86第二季,86不存在的战区第二季")),
            entry(60098L, cn("我的英雄学院 最终季", "我英最终季,英雄学院最终季")),
            entry(1575L, cn("反叛的鲁路修", "叛逆的鲁鲁修,Code Geass")),
            entry(53998L, cn("死神 千年血战篇 诀别谭", "境界千年血战篇诀别谭")),
            entry(51553L, cn("尖帽子的魔法工坊", "尖帽子魔法工房")),
            entry(33095L, cn("昭和元禄落语心中 助六再临篇", "昭和元禄落语心中第二季")),
            entry(51009L, cn("咒术回战 第二季", "咒术第二季,怀玉玉折,涩谷事变")),
            entry(44L, cn("浪客剑心 追忆篇", "神剑闯江湖追忆篇")),
            entry(55690L, cn("我心里危险的东西 第二季", "我心危第二季")),
            entry(21939L, cn("虫师 续章", "虫师第二季")),
            entry(33352L, cn("紫罗兰永恒花园", "薇尔莉特,京紫")),
            entry(44074L, cn("时光代理人", "时光代理人第一季")),
            entry(47778L, cn("鬼灭之刃 游郭篇", "鬼灭游郭篇")),
            entry(61930L, cn("赛马娘 Cinderella Gray Part 2", "赛马娘灰姑娘格雷第二部分")),
            entry(53447L, cn("凸变英雄X", "突变英雄X")),
            entry(245L, cn("麻辣教师GTO", "伟大的教师鬼冢,GTO")),
            entry(61517L, cn("王者天下 第六季", "王国第六季,Kingdom第六季")),
            entry(59192L, cn("鬼灭之刃 无限城篇 第一章 猗窝座再来", "鬼灭无限城,无限城篇")),

            // === 经典名作 ===
            entry(1535L, cn("死亡笔记", "Death Note,小册子,夜神月")),
            entry(30L, cn("新世纪福音战士", "EVA,福音战士,Neon Genesis Evangelism")),
            entry(2001L, cn("天元突破 红莲螺岩", "天元突破,红莲之眼,Gurren Lagann")),
            entry(9756L, cn("魔法少女小圆", "小圆,Madoka Magica")),
            entry(5081L, cn("化物语", "Bakemonogatari,物语系列")),
            entry(10087L, cn("Fate/Zero", "命运之夜,命运前传,FZ")),
            entry(22297L, cn("Fate/stay night 无限剑制", "命运之夜无限剑制,UBW")),
            entry(11757L, cn("刀剑神域", "SAO,Sword Art Online")),
            entry(30276L, cn("一拳超人", "One Punch Man,一击男,琦玉")),
            entry(6547L, cn("Angel Beats!", "天使的心跳,AB")),
            entry(4224L, cn("龙与虎", "Toradora,虎与龙,掌中老虎")),
            entry(5680L, cn("轻音少女", "K-On,KON,轻音")),
            entry(12189L, cn("冰菓", "Hyouka,古典部系列")),
            entry(19815L, cn("游戏人生", "No Game No Life,NGNL")),
            entry(37430L, cn("关于我转生变成史莱姆这档事", "转生史莱姆,萌王")),
            entry(29803L, cn("Overlord", "不死者之王,骨王,安兹")),
            entry(23273L, cn("四月是你的谎言", "四月谎,Your Lie in April")),
            entry(9989L, cn("未闻花名", "那朵花,Anohana,面码")),
            entry(34599L, cn("来自深渊", "Made in Abyss")),
            entry(30831L, cn("为美好的世界献上祝福", "Konosuba,素晴,惠惠")),
            entry(37450L, cn("青春猪头少年不会梦到兔女郎学姐", "青春猪头少年,兔女郎学姐,青猪")),
            entry(37105L, cn("碧蓝之海", "Grand Blue,搞笑潜水")),
            entry(11771L, cn("黑子的篮球", "黑篮,Kuroko no Basket")),
            entry(49596L, cn("蓝色监狱", "Blue Lock,蓝锁")),
            entry(13601L, cn("心理测量者", "Psycho-Pass,心灵判官")),
            entry(22535L, cn("寄生兽 生命的准则", "寄生兽,Parasyte")),
            entry(31043L, cn("只有我不在的街道", "Erased,只有我不存在的城市")),
            entry(46102L, cn("奇巧计程车", "Odd Taxi,ODD TAXI")),
            entry(46095L, cn("薇薇 -萤石眼之歌-", "Vivy,萤石眼之歌")),
            entry(47194L, cn("夏日重现", "Summer Time Rendering,夏日时光")),
            entry(42310L, cn("赛博朋克 边缘行者", "Cyberpunk Edgerunners,边缘行者")),
            entry(50709L, cn("莉可丽丝", "Lycoris Recoil,石蒜反冲")),
            entry(40834L, cn("国王排名", "Ranking of Kings,王様排名")),
            entry(39535L, cn("无职转生", "Mushoku Tensei,无职")),
            entry(14813L, cn("我的青春恋爱物语果然有问题", "春物,大老师,果然我的青春恋爱喜剧搞错了")),
            entry(13759L, cn("樱花庄的宠物女孩", "樱花庄")),
            entry(16067L, cn("来自风平浪静的明天", "Nagi no Asukara,风平浪静")),
            entry(25835L, cn("白箱", "Shirobako")),
            entry(22789L, cn("元气囝仔", "Barakamon")),
            entry(10165L, cn("日常", "Nichijou")),
            entry(11843L, cn("男子高中生的日常", "男高日常")),
            entry(18679L, cn("斩服少女", "Kill la Kill,双斩少女")),
            entry(33489L, cn("小魔女学园", "Little Witch Academia")),
            entry(35849L, cn("DARLING in the FRANXX", "国家队,DitF")),
            entry(28223L, cn("死亡游行", "Death Parade,死亡台球")),

            // === 吉卜力 ===
            entry(431L, cn("哈尔的移动城堡", "Howl's Moving Castle")),
            entry(164L, cn("幽灵公主", "Princess Mononoke,魔法公主")),
            entry(578L, cn("萤火虫之墓", "Grave of the Fireflies")),
            entry(839L, cn("风之谷", "Nausicaa,风之谷的娜乌西卡")),
            entry(513L, cn("天空之城", "Castle in the Sky,拉普达")),
            entry(128L, cn("龙猫", "My Neighbor Totoro,豆豆龙")),
            entry(572L, cn("魔女宅急便", "Kiki's Delivery Service")),
            entry(416L, cn("红猪", "Porco Rosso,飞天红猪侠")),
            entry(585L, cn("侧耳倾听", "Whisper of the Heart,心之谷")),

            // === 近年热门 ===
            entry(50265L, cn("间谍过家家", "Spy x Family,间谍家家酒")),
            entry(44511L, cn("电锯人", "链锯人,Chainsaw Man")),
            entry(52299L, cn("我独自升级", "Solo Leveling")),
            entry(57334L, cn("当哒当", "胆大党,Dandadan")),
            entry(52588L, cn("怪兽8号", "Kaiju No.8,怪8")),
            entry(52701L, cn("迷宫饭", "Dungeon Meshi,舌尖上的地下城")),
            entry(54915L, cn("防风少年", "Wind Breaker")),
            entry(52347L, cn("香格里拉边境", "Shangri-La Frontier,粪作猎人")),
            entry(52211L, cn("物理魔法使马修", "肌肉魔法使,Mashle")),
            entry(38826L, cn("天气之子", "Weathering With You")),
            entry(50594L, cn("铃芽之旅", "Suzume,铃芽户缔")),
            entry(53398L, cn("你想活出怎样的人生", "苍鹭与少年,The Boy and the Heron")),
            entry(40748L, cn("咒术回战", "Jujutsu Kaisen,呪術廻戦")),
            entry(38000L, cn("鬼灭之刃", "Demon Slayer,灶门炭治郎")),
            entry(40456L, cn("鬼灭之刃 无限列车", "鬼灭无限列车,炎柱")),
            entry(54565L, cn("鬼灭之刃 刀匠村篇", "鬼灭刀匠村")),
            entry(55701L, cn("鬼灭之刃 柱训练篇", "鬼灭柱训练")),
            entry(46569L, cn("地狱乐", "Hell's Paradise,地狱楽")),
            entry(54112L, cn("僵尸百分百", "Zom 100,僵尸100")),
            entry(56635L, cn("我推的孩子", "推子,我推,Oshi no Ko")),
            entry(38691L, cn("石纪元", "Dr. Stone,新石纪")),
            entry(37779L, cn("约定的梦幻岛", "The Promised Neverland,梦幻岛")),
            entry(42203L, cn("东京复仇者", "Tokyo Revengers,东卍,东京卍复仇者")),
            entry(51179L, cn("咒术回战 0 剧场版", "咒术0,乙骨忧太")),

            // === 续作与系列 ===
            entry(16498L, cn("进击的巨人", "巨人,Attack on Titan")),
            entry(25777L, cn("进击的巨人 第二季", "巨人第二季")),
            entry(35760L, cn("进击的巨人 第三季", "巨人第三季")),
            entry(28805L, cn("刀剑神域 II", "SAO第二季,幽灵子弹")),
            entry(36474L, cn("刀剑神域 Alicization", "SAO第三季,Underworld")),
            entry(39587L, cn("Re:从零开始的异世界生活 第二季", "Re0第二季,从零开始第二季")),
            entry(41084L, cn("来自深渊 烈日的黄金乡", "来自深渊第二季,Made in Abyss S2")),
            entry(55887L, cn("无职转生 II", "无职转生第二季,Mushoku Tensei S2")),
            entry(39551L, cn("关于我转生变成史莱姆这档事 第二季", "转生史莱姆第二季")),
            entry(32937L, cn("为美好的世界献上祝福 第二季", "Konosuba第二季,素晴第二季")),
            entry(45576L, cn("无职转生 后半", "无职转生第二部")),
            entry(20583L, cn("排球少年", "Haikyuu,小排球,ハイキュー")),
            entry(28891L, cn("排球少年 第二季", "Haikyuu第二季,排球第二季")),
            entry(38249L, cn("排球少年 TO THE TOP", "Haikyuu第四季")),
            entry(40776L, cn("排球少年 TO THE TOP 第二部", "Haikyuu第四季Part2")),
            entry(269L, cn("死神", "BLEACH,境界")),
            entry(1735L, cn("火影忍者 疾风传", "Naruto Shippuden,疾风传")),
            entry(813L, cn("龙珠Z", "Dragon Ball Z,七龙珠Z,DBZ")),
            entry(223L, cn("龙珠", "Dragon Ball,七龙珠")),
            entry(3786L, cn("新世纪福音战士新剧场版：序", "EVA新剧场版序")),
            entry(3785L, cn("新世纪福音战士新剧场版：破", "EVA新剧场版破")),
            entry(3784L, cn("新世纪福音战士新剧场版：Q", "EVA新剧场版Q")),
            entry(19653L, cn("新世纪福音战士新剧场版：终", "EVA新剧场版终")),
            entry(3783L, cn("新世纪福音战士剧场版 Air/真心为你", "EVA真心为你,Air")),
            entry(47904L, cn("王者天下", "Kingdom,王国")),
            entry(38268L, cn("辉夜大小姐想让我告白", "辉夜大小姐,辉夜")),
            entry(40507L, cn("辉夜大小姐想让我告白 第二季", "辉夜大小姐第二季")),
            entry(51219L, cn("灵能百分百", "路人超能100,灵能100")),
            entry(41457L, cn("86 不存在的战区", "86,Eighty Six")),
            entry(51495L, cn("我心里危险的东西", "我心危")),
            entry(33255L, cn("吹响吧！上低音号", "吹响上低音号,京吹")),
            entry(35320L, cn("吹响吧！上低音号 第二季", "京吹第二季,吹响上低音号第二季")),
            entry(239L, cn("浪客剑心", "神剑闯江湖,るろうに剣心")),
            entry(597L, cn("虫师", "Mushishi")),
            entry(34566L, cn("三月的狮子", "3月的狮子,March comes in like a lion")),
            entry(21519L, cn("水果篮子", "水果篮子第一季,生肖奇缘")),
            entry(40434L, cn("水果篮子 第二季", "水果篮子第二季")),
            entry(35839L, cn("Dr.STONE 新石纪", "石纪元第一季,Dr.Stone 石纪元")),
            entry(38414L, cn("总之就是非常可爱", "TONIKAWA,トニカクカワイイ")),

            // === 2026/05/24 全网补全：274条新增中文标题 ===

            // -- 高人气热门 --
            entry(32182L, cn("灵能百分百", "路人超能100,MOB")),
            entry(37999L, cn("辉夜大小姐想让我告白～天才们的恋爱头脑战～", "辉夜大小姐,辉夜,辉夜大小姐想让我告白第一季,恋爱头脑战")),
            entry(35790L, cn("盾之勇者成名录", "盾勇,盾之勇者,尚文")),
            entry(30654L, cn("暗杀教室 第二季", "暗杀教室2,三年E班,杀老师")),
            entry(40591L, cn("辉夜大小姐想让我告白？～天才们的恋爱头脑战～", "辉夜大小姐第二季,辉夜2,恋爱头脑战2")),
            entry(205L, cn("混沌武士", "琉球武士疯云录,武士大杂烩")),
            entry(48561L, cn("咒术回战0 剧场版", "咒术回战零,咒术0,乙骨忧太")),
            entry(11741L, cn("Fate/Zero 第二季", "命运零点第二季,FZ第二季")),
            entry(36098L, cn("我想吃掉你的胰脏", "胰脏,我想吃掉你的胰脏")),
            entry(38883L, cn("排球少年!! TO THE TOP", "排球少年第四季,小排球4")),
            entry(32615L, cn("幼女战记", "谭雅战记,幼女战记")),
            entry(32L, cn("新世纪福音战士剧场版：Air/真心为你", "EVA剧场版,新世纪福音战士剧场版,真心为你,EVA旧剧场版")),
            entry(37675L, cn("Overlord 第三季", "不死者之王3,骨傲天3")),
            entry(2251L, cn("永生之酒", "BACCANO,永生之酒")),
            entry(30484L, cn("命运石之门0", "石头门0,命运石之门零")),
            entry(33674L, cn("游戏人生ZERO", "NO GAME NO LIFE剧场版,游戏人生剧场版,NGNL剧场版")),
            entry(16894L, cn("黑子的篮球 第二季", "黑蓝2,影子篮球2")),
            entry(457L, cn("虫师", "MUSHISHI,蟲師")),
            entry(38040L, cn("为美好的世界献上祝福！红传说", "素晴剧场版,为美好世界献上祝福剧场版,KONOSUBA剧场版")),
            entry(48316L, cn("想要成为影之实力者", "影之实力者,影实,影之强者")),
            entry(1210L, cn("欢迎加入NHK！", "NHK,家里蹲,欢迎来到NHK")),
            entry(24415L, cn("黑子的篮球 第三季", "黑蓝3,影子篮球3")),
            entry(38329L, cn("青春猪头少年不会梦到怀梦美少女", "青猪剧场版,青春猪头,兔女郎学姐剧场版")),
            entry(39547L, cn("我的青春恋爱物语果然有问题。完", "春物第三季,大老师3,果然我的青春恋爱喜剧搞错了完")),
            entry(58567L, cn("我独自升级 第二季 -起于暗影-", "我独自升级2,我独2,Solo Leveling S2")),
            entry(33L, cn("剑风传奇", "烙印战士,烙印勇士,狂战士格斯")),
            entry(7791L, cn("轻音少女 第二季", "K-ON第二季,轻音!!,KON2")),
            entry(34612L, cn("齐木楠雄的灾难 第二季", "齐木楠雄2,齐神2")),
            entry(7311L, cn("凉宫春日的消失", "凉宫春日剧场版,凉宫春日的消失")),
            entry(136L, cn("全职猎人", "猎人,富坚义博,HUNTER×HUNTER")),
            entry(11577L, cn("命运石之门 剧场版：负荷领域的既视感", "石头门剧场版,命运石之门剧场版")),
            entry(39533L, cn("GIVEN 被赠与的未来", "GIVEN,被赠与的未来,吉缘")),
            entry(48895L, cn("Overlord 第四季", "不死者之王4,骨傲天4")),
            entry(31339L, cn("漂流者", "漂流武士,DRIFTERS")),
            entry(39247L, cn("小林家的龙女仆S", "小林家的龙女仆第二季,妹抖龙S,小林家的妹抖龙2")),
            entry(6594L, cn("刀语", "刀语,KATANAGATARI")),
            entry(4081L, cn("夏目友人帐", "夏目,夏目友人帐第一季,友人帐")),
            entry(40902L, cn("食戟之灵 第五季", "食戟之灵5,春药之灵5,豪之皿")),
            entry(40417L, cn("水果篮子 第二季", "水果篮子2,生肖奇缘2")),
            entry(9260L, cn("伤物语I：铁血篇", "伤物语铁血篇,伤物语1,物语系列")),
            entry(34798L, cn("摇曳露营△", "摇曳露营第一季,露营,△露营,YURU CAMP")),
            entry(36862L, cn("来自深渊 深沉灵魂的黎明", "来自深渊剧场版,深沉灵魂的黎明,MADE IN ABYSS剧场版")),
            entry(40787L, cn("乔西的虎与鱼", "JOSEE,乔西与虎与鱼,约瑟与虎与鱼")),
            entry(10162L, cn("白兔糖", "白兔糖,兔子糖,USAGI DROP")),
            entry(45L, cn("浪客剑心", "神剑闯江湖,剑心,绯村剑心")),
            entry(5341L, cn("狼与香辛料II", "狼与辛香料2,贤狼赫萝2")),
            entry(35557L, cn("宝石之国", "宝石之国,磷叶石")),
            entry(31181L, cn("终物语", "OWARIMONOGATARI,终物语,物语系列")),
            entry(54857L, cn("Re:从零开始的异世界生活 第三季", "RE0第三季,从零开始3,REZERO第三季")),
            entry(57658L, cn("咒术回战：死灭回游·前篇", "咒术回战第三季,咒术3,死灭回游")),
            entry(10800L, cn("花牌情缘", "歌牌情缘,花牌,CHIHAYAFURU")),
            entry(7785L, cn("四畳半神话大系", "四叠半神话大系,四叠半,四叠半宿舍")),
            entry(35851L, cn("朝花夕誓——于离别之朝束起约定之花", "朝花夕誓,MARQUIA,离别之朝束起约定之花")),
            entry(31757L, cn("伤物语II：热血篇", "伤物语热血篇,伤物语2,物语系列")),
            entry(17549L, cn("悠哉日常大王", "日常大王,悠哉日常,NON NON BIYORI")),
            entry(59845L, cn("薰香花朵凛然绽放", "薫香花朵,凛然绽放,薰花")),
            entry(32380L, cn("为美好的世界献上祝福！为美好的项圈献上祝福！", "素晴OVA,KONOSUBA OVA")),
            entry(22135L, cn("乒乓", "乒乓,桌球,PING PONG")),
            entry(33049L, cn("命运之夜——天之杯II：迷失之蝶", "FATE HF2,天之杯2,迷失之蝶")),
            entry(66L, cn("阿滋漫画大王", "阿滋漫画大王,校园漫画大王")),
            entry(467L, cn("攻壳机动队 STAND ALONE COMPLEX", "攻壳机动队SAC,攻壳SAC,STAND ALONE COMPLEX")),
            entry(12531L, cn("坂道上的阿波罗", "坂道上的阿波罗,爵士,APOLLON")),
            entry(5L, cn("星际牛仔：天国之门", "COWBOY BEBOP剧场版,赏金猎人剧场版")),
            entry(33050L, cn("命运之夜——天之杯III：春之歌", "FATE HF3,天之杯3,春之歌")),
            entry(34626L, cn("为美好的世界献上祝福！2 为美好的艺术献上祝福！", "素晴第二季OVA,KONOSUBA第二季OVA")),
            entry(35838L, cn("少女终末旅行", "少女终末旅行,终末旅行")),
            entry(48849L, cn("漂流少年", "SONNY BOY,漂流少年")),
            entry(57L, cn("摇滚新乐团", "BECK,摇滚新乐团")),
            entry(39468L, cn("爱书的下克上：为了成为图书管理员不择手段", "爱书的下克上,小书痴,书虫的下克上")),
            entry(10030L, cn("爆漫王。第二季", "食梦者2,BAKUMAN2,爆漫王2")),
            entry(2246L, cn("怪化猫", "物怪,MONONOKE,怪化猫")),
            entry(34636L, cn("舞动青春", "BALLROOM,社交舞,舞蹈竞技")),
            entry(50330L, cn("文豪野犬 第四季", "文豪野犬4,文野4,文豪Stray Dogs4")),
            entry(12365L, cn("爆漫王。第三季", "食梦者3,BAKUMAN3,爆漫王3")),
            entry(32828L, cn("天真与闪电", "甜蜜与闪电,甘々と稲妻")),
            entry(53446L, cn("网购技能开启异世界美食之旅", "网购异世界美食,痴汉技能,网购技能")),
            entry(170L, cn("灌篮高手", "篮球飞人,樱木花道,SLAM DUNK,男儿当入樽")),
            entry(37965L, cn("强风吹拂", "强风,跑步,箱根驿传")),
            entry(49889L, cn("月光下的异世界之旅 第二季", "月道2,月光异世界2")),
            entry(1698L, cn("交响情人梦", "NODAME,交响情人梦,野田妹")),
            entry(57181L, cn("蓝箱", "青春之箱,BLUE BOX,青色箱子")),
            entry(50380L, cn("派对浪客诸葛孔明", "派对孔明,诸葛孔明,派对浪客")),
            entry(34240L, cn("Shelter", "波特·罗宾逊,SHELTER,避难所")),
            entry(5258L, cn("第一神拳 第二季 New Challenger", "第一神拳2,拳击,一步2")),
            entry(38084L, cn("命运-冠位指定 绝对魔兽战线 巴比伦尼亚", "FGO巴比伦尼亚,绝对魔兽战线,FGO第七特异点")),
            entry(56784L, cn("死神 千年血战篇-相克谭-", "BLEACH千年血战篇第三季,死神千年血战相克谭")),
            entry(34012L, cn("异世界食堂", "异世界餐厅,異世界食堂")),
            entry(11665L, cn("夏目友人帐 肆", "夏目友人帐第四季,夏目4")),
            entry(40421L, cn("GIVEN 剧场版", "GIVEN剧场版,被赠与的未来剧场版")),
            entry(54898L, cn("文豪野犬 第五季", "文豪野犬5,文野5")),

            // -- 经典 & 高分 --
            entry(31988L, cn("吹响吧！上低音号2", "京吹2,吹响上低音号第二季,悠风号2")),
            entry(5040L, cn("超智游戏", "ONE OUTS,超智游戏,赌博棒球")),
            entry(40815L, cn("爱书的下克上 第二季", "小书痴2,书虫2,爱书2")),
            entry(37208L, cn("魔道祖师", "魔道祖师,魏无羡,蓝忘机,MDZS")),
            entry(4282L, cn("空之境界 第五章：矛盾螺旋", "空之境界5,空境5,矛盾螺旋")),
            entry(14397L, cn("花牌情缘2", "歌牌情缘2,花牌情缘第二季")),
            entry(36999L, cn("续·终物语", "续终物语,物语系列,ZOKU OWARI")),
            entry(35737L, cn("冥王", "PLUTO,冥王,浦泽直树")),
            entry(58125L, cn("蓦然回首", "LOOK BACK,藤本树,蓦然回首")),
            entry(58390L, cn("夜曲 第二季", "彻夜之歌2,夜曲2,熬夜之歌2")),
            entry(801L, cn("攻壳机动队 S.A.C. 2nd GIG", "攻壳SAC 2ND GIG,攻壳机动队第二季TV")),
            entry(4565L, cn("天元突破 剧场版 螺岩篇", "天元突破剧场版2,红莲篇续,螺岩篇")),
            entry(40729L, cn("NOMAD MEGALO BOX 2", "装甲重拳2,NOMAD,流浪装甲重拳2")),
            entry(52742L, cn("排球少年!! 垃圾场的决战", "排球少年剧场版,垃圾场决战,小排球剧场版,猫与鸦")),
            entry(3702L, cn("底特律金属城", "DMC,重金属,根岸")),
            entry(61322L, cn("Dr.STONE 科学未来 第二部分", "石纪元4,DR.STONE第四季")),
            entry(35843L, cn("银魂 走光篇", "银魂第四季,银魂走光篇,银魂.")),
            entry(5205L, cn("空之境界 第七章：杀人考察（后）", "空之境界7,空境7,杀人考察后")),
            entry(23623L, cn("悠哉日常大王 Repeat", "悠哉日常大王第二季,悠哉日常2")),
            entry(50360L, cn("无职转生～到了异世界就拿出真本事～艾莉丝的哥布林讨伐", "无职转生OVA,艾莉丝OVA,无职OVA")),
            entry(49721L, cn("擅长捉弄的高木同学 第三季", "高木同学3,擅长捉弄的高木3,高木3")),
            entry(12431L, cn("宇宙兄弟", "宇宙兄弟,UCHUU KYOUDAI,太空兄弟")),
            entry(12403L, cn("摇曳百合♪♪", "摇曳百合第二季,轻松百合2,YURU YURI2")),
            entry(49909L, cn("小太郎一个人生活", "小太郎,KOTARO,一个人住")),
            entry(49818L, cn("诡秘之主", "LORD OF MYSTERIES,诡秘,克莱恩")),
            entry(36885L, cn("路人女主的养成方法 Fine", "路人女主剧场版,SAEKANO剧场版,加藤惠剧场版")),
            entry(38889L, cn("一弦定音！第二季", "KONO OTO TOMARE2,古筝,一弦定音2")),
            entry(36754L, cn("妖怪旅馆营业中", "妖怪旅馆,隐世旅馆,天神屋")),
            entry(59986L, cn("碧蓝之海 第二季", "碧蓝之海2,GRAND BLUE2")),
            entry(42429L, cn("爱书的下克上 第三季", "小书痴3,书虫3,爱书3")),
            entry(3167L, cn("夏娃的时间", "EVE,夏娃的时间,机器人咖啡店")),
            entry(11553L, cn("龙与虎！便当的极致", "龙与虎SP,虎与龙OVA,TORADORA OVA")),
            entry(60371L, cn("正相反的你和和我", "正反的你和我,性格相反的我们")),
            entry(54870L, cn("青春猪头少年不会梦到背包女孩", "青猪背包女孩,青春猪头新剧场版,兔女郎学姐新剧场版")),
            entry(30709L, cn("元气少女缘结神 过去篇", "元气少女缘结神OVA,过去篇,巴卫")),
            entry(59970L, cn("关于我转生变成史莱姆这档事 第四季", "转生史莱姆4,萌王4,史莱姆4")),
            entry(7472L, cn("银魂剧场版：新译红樱篇", "银魂剧场版1,新译红樱篇,银魂红樱")),
            entry(40730L, cn("天官赐福", "天官赐福,谢怜,花城,HEAVENS BLESSING")),
            entry(34480L, cn("食戟之灵 贰之皿 OVA", "食戟之灵OVA,春药之灵OVA")),
            entry(153L, cn("十二国记", "十二国记,JUUNI KOKUKI")),
            entry(6862L, cn("轻音少女 Live House!", "KON OVA,轻音OVA,LIVE HOUSE")),
            entry(37515L, cn("来自深渊 漂泊的黄昏", "来自深渊剧场版2,漂泊的黄昏")),
            entry(35677L, cn("利兹与青鸟", "LIZ与青鸟,京吹剧场版,莉兹与青鸟")),
            entry(264L, cn("第一神拳 冠军之路", "第一神拳剧场版,冠军之路,一步剧场版")),
            entry(37379L, cn("花牌情缘3", "歌牌情缘3,花牌情缘第三季")),
            entry(21329L, cn("虫师 日蚀翳", "虫师特别篇,日蚀翳,MUSHISHI SP")),
            entry(4472L, cn("幸运星 OVA", "LUCKY STAR OVA,脑残星OVA")),
            entry(30230L, cn("钻石王牌 第二季", "钻A2,钻石王牌2,ダイヤのA2")),
            entry(3297L, cn("水星领航员 第三季", "水星领航员,ARIA第三季,ARIA ORIGINATION")),
            entry(28957L, cn("虫师 续章 铃之滴", "虫师铃之滴,虫师特别篇")),

            // -- 运动/竞技/美食 --
            entry(10033L, cn("美食的俘虏", "美食猎人,阿虏,美食的俘虏")),
            entry(39808L, cn("悠哉日常大王 Nonstop", "悠哉日常大王第三季,悠哉日常3,NONSTOP")),
            entry(53407L, cn("调酒师 神之杯", "调酒师2024,神之杯,BARTENDER新作")),
            entry(9734L, cn("轻音少女!! 计划！", "K-ON OVA,轻音SP,轻音计划")),
            entry(5690L, cn("交响情人梦 最终篇", "交响情人梦FINALE,野田妹最终篇")),
            entry(30279L, cn("摇曳百合 第三季", "摇曳百合3,轻松百合3,YURU YURI第三季")),
            entry(5941L, cn("四叶游戏", "幸运四叶草,棒球,安达充")),
            entry(4477L, cn("交响情人梦 巴黎篇", "交响情人梦PARIS篇,野田妹巴黎")),
            entry(31327L, cn("食戟之灵 OVA", "食戟之灵OVA,春药之灵OVA,食戟SP")),
            entry(265L, cn("第一神拳 间柴vs木村", "第一神拳特别篇,间柴VS木村,一步特别篇")),
            entry(38450L, cn("魔道祖师 第二季", "魔道祖师2,魔道祖师羡云篇")),
            entry(56538L, cn("好想告诉你 第三季", "只想告诉你3,好想告诉你3")),
            entry(2402L, cn("明日之丈", "明日之丈,铁拳浪子,矢吹丈")),
            entry(24687L, cn("虫师 续章 荆棘之路", "虫师荆棘之路,虫师特别篇,棘之路")),
            entry(42941L, cn("赛马娘 Pretty Derby 第二季", "赛马娘2,马娘2,UMA MUSUME第二季")),
            entry(35110L, cn("排球少年 剧场版：才能与感觉", "排球少年剧场版3,才能与感觉")),
            entry(627L, cn("棒球大联盟 第一季", "MAJOR 1,棒球大联盟1,茂野吾郎")),
            entry(1589L, cn("调酒师", "BARTENDER,王牌酒保,调酒师2006")),
            entry(35111L, cn("排球少年 剧场版：概念之战", "排球少年剧场版4,概念之战")),
            entry(57864L, cn("物语系列 外传&怪物季", "物语系列新作,MONOGATARI OFF MONSTER,物语怪物季")),
            entry(53410L, cn("摇曳露营△ 第三季", "摇曳露营3,露营3,YURU CAMP3")),
            entry(61903L, cn("辉夜大小姐想让我告白：大人的阶梯", "辉夜大小姐最终章,辉夜大人篇")),
            entry(49053L, cn("GIVEN 里侧的存在", "GIVEN OVA,GIVEN ON THE OTHER HAND")),
            entry(38731L, cn("钻石王牌 actII", "钻A第三季,钻石王牌第三季,ACT II")),
            entry(2685L, cn("翼·年代记 东京默示录", "翼年代记OVA,东京默示录,TSUBASA Tokyo")),
            entry(49722L, cn("擅长捉弄的高木同学 剧场版", "高木同学剧场版,擅长捉弄的高木剧场版")),
            entry(38475L, cn("摇曳露营△ 剧场版", "露营剧场版,摇曳露营剧场版,YURU CAMP剧场版")),
            entry(11739L, cn("少年同盟2", "少年同盟第二季,KIMI TO BOKU2")),
            entry(44087L, cn("银魂 THE SEMI-FINAL", "银魂半决赛篇,银魂特别篇,SEMI-FINAL")),
            entry(55318L, cn("舞冰的祈愿", "MEDALIST,冰上舞者,花滑")),
            entry(62896L, cn("超·辉夜姬！", "COSMIC PRINCESS KAGUYA,超辉夜姬")),
            entry(59791L, cn("瑠璃的宝石", "RURI ROCKS,瑠璃宝石,琉璃宝石")),
            entry(20651L, cn("夏目友人帐：曾几何时下雪之日", "夏目友人帐OVA,下雪之日,雪日夏目")),
            entry(36990L, cn("悠哉日常大王 剧场版：假期", "悠哉日常大王剧场版,假期,VACATION")),
            entry(962L, cn("水星领航员 第二季", "ARIA第二季,水星领航员NATURAL,ARIA NATURAL")),
            entry(45556L, cn("伍六七之玄武国篇", "刺客伍六七玄武国,五六七3,剪刀刺客")),
            entry(10937L, cn("机动战士高达 THE ORIGIN", "高达THE ORIGIN,高达起源,0079起源")),
            entry(36538L, cn("夏目友人帐 剧场版：结缘空蝉", "夏目友人帐剧场版,结缘空蝉,夏目剧场版")),

            // -- 剧场版/OVA/短片 --
            entry(731L, cn("星际5555：异星梦系统秘传", "INTERSTELLA5555,傻朋克,DAFT PUNK")),
            entry(59833L, cn("为美好的世界献上祝福！3 特别篇", "素晴3OVA,KONOSUBA3OVA")),
            entry(19511L, cn("火影忍者疾风传 Sunny Side Battle", "火影OVA,鸣人特别篇,阳光番外")),
            entry(23225L, cn("摇曳百合 暑假时光！", "摇曳百合暑假,摇曳百合OVA2,夏日合宿")),
            entry(3001L, cn("萌菌物语", "萌菌物语,MOYASHIMON,菌物语")),
            entry(42745L, cn("街角魔族 2丁目", "街角魔族第二季,街角魔族2,魔族2丁目")),
            entry(558L, cn("棒球大联盟 第二季", "MAJOR 2,棒球大联盟2")),
            entry(5028L, cn("棒球大联盟 第五季", "MAJOR 5,棒球大联盟5")),
            entry(6945L, cn("银魂 白夜叉降诞", "银魂JSA2008,白夜叉降诞,银魂特别篇")),
            entry(1842L, cn("棒球大联盟 第三季", "MAJOR 3,棒球大联盟3")),
            entry(55310L, cn("新上司很天然", "天然上司,新上司很自然,新上司天然呆")),
            entry(39730L, cn("放学后堤防日志", "放学后海堤日记,堤防日志,钓鱼部")),
            entry(38337L, cn("请问您今天要来点兔子吗？BLOOM", "点兔第三季,点兔BLOOM,兔子BLOOM")),
            entry(55357L, cn("孤独摇滚！剧场版", "BOCCHI THE ROCK剧场版,波奇酱剧场版,孤独摇滚总集篇")),
            entry(58919L, cn("100m", "一百米,百米,HYAKUEMU")),
            entry(57466L, cn("爱书的下克上 领主的养女", "小书痴第四季,爱书4,领主养女")),
            entry(3226L, cn("棒球大联盟 第四季", "MAJOR 4,棒球大联盟4")),
            entry(57779L, cn("异兽魔都 第二季", "DOROHEDORO 2,异兽魔都2,林田球")),
            entry(7655L, cn("棒球大联盟 第六季", "MAJOR 6,棒球大联盟6")),
            entry(50399L, cn("天官赐福 第二季", "天官赐福2,谢怜2,花城2")),
            entry(51440L, cn("佐佐木与宫野 毕业篇", "佐佐木宫野剧场版,毕业篇")),
            entry(44070L, cn("天官赐福 特别篇", "天官特别篇,天官赐福SP")),
            entry(57647L, cn("赛马娘 Pretty Derby 新时代之门", "赛马娘剧场版,马娘新时代之门,UMA MUSUME剧场版")),
            entry(33970L, cn("少女与战车 最终章 第1话", "少战最终章1,少女与战车最终章,GIRLS UND PANZER FINALE")),
            entry(63019L, cn("棱镜回旋曲", "PRISM RONDO,恋はプリズム")),
            entry(59897L, cn("小林家的龙女仆 寂寞的龙", "妹抖龙剧场版,小林龙女仆剧场版,寂寞的龙")),
            entry(36275L, cn("夏目友人帐 陆 特别篇", "夏目6SP,夏目第六季SP")),
            entry(42894L, cn("夏目友人帐 石起和可疑来访者", "夏目友人帐特别篇,石起,可疑来访者")),
            entry(32547L, cn("悠哉日常大王 Repeat 萤乐在其中", "悠哉日常第二季OVA,萤乐在其中")),
            entry(34534L, cn("夏目友人帐 伍 特别篇", "夏目5SP,夏目第五季SP")),
            entry(34420L, cn("异世界居酒屋 阿伊特利亚的居酒屋信", "异世界居酒屋,居酒屋NOBU,阿信")),
            entry(5984L, cn("天堂餐馆", "RISTORANTE PARADISO,天堂餐馆")),
            entry(54791L, cn("GIVEN 剧场版 柊MIX", "GIVEN 柊MIX,柊MIX,GIVEN剧场版2")),
            entry(21899L, cn("银魂 精选集 2D影院版", "银魂精选集,2D剧场版,精选集")),
            entry(3604L, cn("向阳素描×365", "向阳素描第二季,向阳365,暖阳涂鸦2")),
            entry(33280L, cn("请问您今天要来点兔子吗？？Dear My Sister", "点兔OVA,兔子DEAR MY SISTER,点兔SP")),
            entry(38081L, cn("少女与战车 最终章 第2话", "少战最终章2,少女与战车最终章2")),
            entry(9441L, cn("梦色糕点师SP Professional", "梦色糕点师SP,梦色蛋糕师SP")),

            // -- 音乐/偶像/乐队 --
            entry(32843L, cn("战姬绝唱SYMPHOGEAR XV", "SYMPHOGEAR第五季,战姬绝唱5,XV")),
            entry(37870L, cn("BanG Dream! 第三季", "BANG DREAM第三季,邦邦3")),
            entry(54959L, cn("BanG Dream! It's MyGO!!!!!", "MYGO,迷星叫,BANG DREAM MYGO")),
            entry(2563L, cn("水星领航员 OVA ～亚利埃塔～", "ARIA OVA,水星领航员OVA,ARIETTA")),
            entry(11917L, cn("棒球大联盟 世界大赛篇", "MAJOR WORLD SERIES,棒球大联盟世界大赛")),
            entry(49521L, cn("悠哉日常大王 Nonstop 社团活动加油", "悠哉日常第三季OVA,社团活动")),
            entry(50159L, cn("佐贺偶像是传奇 剧场版 梦银河乐园", "佐贺偶像剧场版,ZOMBIE LAND SAGA剧场版,梦银河乐园")),
            entry(36513L, cn("鹿枫堂四色日和", "鹿枫堂,和风咖啡馆,YOTSUIRO BIYORI")),
            entry(56653L, cn("BanG Dream! Ave Mujica", "AVE MUJICA,颂乐人偶,BANG DREAM MUJICA")),
            entry(37962L, cn("IDOLiSH7 第二季", "爱娜娜第二季,偶像星愿2,SECOND BEAT")),

            // -- 国产/轻改 --
            entry(37618L, cn("萌妻食神", "萌妻食神,美食恋爱")),
            entry(2150L, cn("姆明一族", "姆明,MOOMIN,噜噜米")),
            entry(1078L, cn("魔卡少女樱 知世的活跃日记", "小樱OVA,知世篇,库洛魔法使")),
            entry(42984L, cn("Gotcha!", "宝可梦MV,GOTCHA,精灵宝可梦")),
            entry(7062L, cn("向阳素描×☆☆☆", "向阳素描第三季,向阳素描三星,暖阳涂鸦3")),
            entry(39112L, cn("知晓天空之蓝的人啊", "空青,空の青さ,超平和Busters")),
            entry(9563L, cn("向阳素描×☆☆☆ 特别篇", "向阳素描三星SP,向阳素描SP2")),
            entry(11239L, cn("向阳素描×蜂巢", "向阳素描第四季,向阳素描HONEYCOMB,暖阳涂鸦4")),
            entry(45577L, cn("IDOLiSH7 第三季", "爱娜娜第三季,偶像星愿3,THIRD BEAT")),
            entry(40664L, cn("少女☆歌剧 Revue Starlight 剧场版", "少歌剧场版,REVUE STARLIGHT剧场版,再生产")),
            entry(62927L, cn("放开那个女巫", "放开女巫,女巫异能,RELEASE THAT WITCH")),
            entry(4772L, cn("水星领航员 那个小小的秘密的地方…", "ARIA SP,水星领航员SP,秘密场所")),
            entry(110L, cn("中华小当家", "中华一番,小当家,特级厨师")),
            entry(50250L, cn("吉伊卡哇", "小可爱,CHIIKAWA,吉伊")),
            entry(48653L, cn("向夜晚奔去", "YOASOBI,夜に駆ける,冲向夜晚")),
            entry(53540L, cn("名侦探柯南 黑铁的鱼影", "柯南剧场版26,黑铁的鱼影,M26")),
            entry(56175L, cn("大室家 dear sisters", "大室家,摇曳百合外传,OOMURO-KE")),
            entry(59001L, cn("成何体统", "成何体统,穿越,古装")),
            entry(17739L, cn("向阳素描 沙英·寻毕业篇", "向阳素描毕业篇,沙英寻毕业,HIDAMARI SKETCH毕业")),
            entry(55255L, cn("外星人舞台", "ALIEN STAGE,外星舞台,音乐企划")),
            entry(34290L, cn("闪耀☆光之美少女 a la mode", "KIRAKIRA光美,光之美少女甜点,Q娃")),
            entry(25729L, cn("玉响 毕业写真 第1部 芽生", "玉响毕业写真1,芽生,TAMAYURA")),
            entry(29831L, cn("玉响 毕业写真 第4部 明日", "玉响毕业写真4,明日,TAMAYURA")),
            entry(41782L, cn("BanG Dream! 剧场版 Poppin'Dream!", "BANG DREAM剧场版,POPPIN DREAM,邦邦剧场版")),
            entry(47794L, cn("MILGRAM", "MILGRAM,音乐企划,囚犯审判")),
            entry(41781L, cn("BanG Dream! 剧场版 Episode of Roselia II: Song I am.", "BANG DREAM ROSELIA剧场版,SONG I AM")),
            entry(29829L, cn("玉响 毕业写真 第2部 响", "玉响毕业写真2,响,TAMAYURA2")),
            entry(29830L, cn("玉响 毕业写真 第3部 憧", "玉响毕业写真3,憧,TAMAYURA3")),
            entry(48411L, cn("水星领航员 The BENEDIZIONE", "ARIA剧场版,BENEDIZIONE,水星领航员最终章")),
            entry(49847L, cn("萌妻食神 第二季", "萌妻食神2,再结良缘")),
            entry(60988L, cn("天官赐福 短篇集", "天官短篇,天官赐福SP,天官小剧场")),
            entry(61952L, cn("罗小黑战记2", "罗小黑2,小黑2,LEGEND OF HEI 2")),
            entry(60544L, cn("哪吒之魔童闹海", "哪吒2,魔童闹海,NE ZHA 2")),
            entry(55809L, cn("仙逆", "XIAN NI,RENEGADE IMMORTAL,王林")),
            entry(57993L, cn("大室家 dear friends", "大室家第二部,大室家FRIENDS,OOMURO-KE2")),
            entry(41462L, cn("BanG Dream! Film Live 2nd Stage", "BANG DREAM FILM LIVE 2,邦邦演唱会2")),
            entry(59817L, cn("少女乐队哭 剧场版", "GBC剧场版,少女乐队,哭泣乐队剧场版")),
            entry(42206L, cn("崩坏3 女武神的餐桌II", "女武神的餐桌2,崩坏三,COOKING WITH VALKYRIES2")),
            entry(51203L, cn("萌妻食神 第三季", "萌妻食神3,欢喜追婚")),
            entry(17113L, cn("蜡笔小新 笨蛋好吃！B级美食大逃亡", "蜡笔小新剧场版21,B级美食,小新2013")),
            entry(1093L, cn("美味大挑战", "OISHINBO,美味大挑战,美食漫画")),
            entry(56524L, cn("吞噬星空 第四季", "吞噬星空4,SWALLOWED STAR 4,罗峰")),
            entry(3437L, cn("妙手小厨师", "MISTER AJIKKO,味っ子,妙手小厨师")),
            entry(56523L, cn("吞噬星空 第三季", "吞噬星空3,SWALLOWED STAR 3,罗峰")),
            entry(58930L, cn("原神：卫星之路", "原神动画,原神短片,GENSHIN IMPACT")),
            entry(8123L, cn("伙头仔昆布", "COOKING PAPA,伙头仔昆布,料理爸爸")),
            entry(60557L, cn("凡人修仙传 第四季", "凡人修仙传4,凡人4,韩立")),
            entry(54014L, cn("李林克的小馆儿 第二季", "李林克2,小馆儿2,LINK LEE2"))
    );

    @Transactional
    @CacheEvict(cacheNames = {"animeList", "animeDetail", "animeSeason", "ranking"}, allEntries = true)
    public int seedChineseTitles() {
        int updated = 0;

        for (Map.Entry<Long, ChineseTitle> entry : CHINESE_TITLES.entrySet()) {
            Optional<Anime> maybeAnime = animeRepository.findByMalId(entry.getKey());
            if (maybeAnime.isEmpty()) {
                continue;
            }

            Anime anime = maybeAnime.get();
            ChineseTitle cn = entry.getValue();
            String nextAliases = mergeAliases(anime.getSearchAliases(), cn.titleCn(), cn.aliases());
            boolean changed = false;

            if (isBlank(anime.getTitleCn()) || !anime.getTitleCn().equals(cn.titleCn())) {
                anime.setTitleCn(cn.titleCn());
                changed = true;
            }

            if (!nextAliases.equals(nullToEmpty(anime.getSearchAliases()))) {
                anime.setSearchAliases(nextAliases);
                changed = true;
            }

            if (changed) {
                animeRepository.save(anime);
                updated++;
            }
        }

        if (updated > 0) {
            log.info("已补全 {} 条动漫中文搜索名", updated);
        }

        return updated;
    }

    private static ChineseTitle cn(String titleCn, String aliases) {
        return new ChineseTitle(titleCn, aliases);
    }

    private static String mergeAliases(String existing, String titleCn, String aliases) {
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        addAliases(merged, existing);
        addAliases(merged, titleCn);
        addAliases(merged, aliases);
        return String.join("，", merged);
    }

    private static void addAliases(Set<String> merged, String rawAliases) {
        if (isBlank(rawAliases)) {
            return;
        }
        for (String alias : rawAliases.split("[,，、;；|/\\n]+")) {
            String normalized = alias.trim();
            if (!normalized.isEmpty() && normalized.length() <= 120) {
                merged.add(normalized);
            }
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private record ChineseTitle(String titleCn, String aliases) {
    }
}
