package con.aidenjia.digitalhuman.conversation.infrastructure.avatar;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import con.aidenjia.digitalhuman.conversation.avatar.AvatarDecisionInput;
import con.aidenjia.digitalhuman.conversation.config.AvatarDecisionProperties;
import con.aidenjia.digitalhuman.conversation.config.BailianRealtimeProperties;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Opt-in, billable semantic check using synthetic inputs and the production HTTP adapter. */
@EnabledIfEnvironmentVariable(named = "AVATAR_DECISION_LIVE_TEST", matches = "true")
class QwenAvatarDecisionLiveTest {
    private static final OkHttpClient HTTP = new OkHttpClient();

    @AfterAll
    static void closeHttp() {
        HTTP.dispatcher().executorService().shutdown();
        HTTP.connectionPool().evictAll();
    }

    static Stream<Arguments> greetings() {
        String selected = System.getenv("AVATAR_DECISION_PROBE_CASE");
        return Stream.of(
                        Arguments.of("teasing", "你好", "哼，谁要跟你打招呼啦？", "none", "greet"),
                        Arguments.of("indirect", "早上好", "你怎么才来呀！", "none", "greet"),
                        Arguments.of("ordinary", "你好呀", "你好，很高兴见到你！", "none", "greet"),
                        Arguments.of("farewell", "再见", "这就走啦？", "none", "greet"),
                        Arguments.of("translation", "把你好翻译成英语", "你好可以翻译成 hello。", "none", "none"),
                        Arguments.of("third-party", "同事刚才对我说你好", "他在向你打招呼呢。", "none", "none"),
                        Arguments.of("refusal", "你好", "别打招呼了，我现在不想聊天。", "none", "none"),
                        Arguments.of("explicit", "你好，向我挥挥手", "好呀，这就挥手。", "none", "none"),
                        Arguments.of("cooldown", "你好", "哼，谁要跟你打招呼啦？", "greet", "none"),
                        Arguments.of("explanation", "为什么人见面会说你好", "这是一种社交礼仪。", "none", "none"),
                        Arguments.of(
                                "agreement", "我理解为先准备素材再接入，对吗", "对，你理解得完全正确！", "none", "agree"),
                        Arguments.of(
                                "agreement-cooldown", "这样理解对吗", "对，你理解得完全正确！", "agree", "none"),
                        Arguments.of(
                                "explain-reason",
                                "为什么天空是蓝色的",
                                "因为空气对不同颜色的光散射程度不同。",
                                "none",
                                "explain"),
                        Arguments.of(
                                "explain-steps",
                                "怎么整理这件事",
                                "可以分成两步，先列出问题，再逐个确认。",
                                "none",
                                "explain"),
                        Arguments.of(
                                "explain-mechanism",
                                "缓存为什么能变快",
                                "缓存把经常使用的结果保存下来，下一次就能直接读取。",
                                "none",
                                "explain"),
                        Arguments.of(
                                "explain-introduction",
                                "能解释一下吗",
                                "这个问题可以从两个方面来看。",
                                "none",
                                "explain"),
                        Arguments.of(
                                "explain-after-agreement",
                                "是不是先加载素材再播放",
                                "对，因为提前加载可以避免播放时等待网络。",
                                "none",
                                "explain"),
                        Arguments.of(
                                "explain-cooldown",
                                "缓存为什么快",
                                "缓存省去了重复计算和读取的时间。",
                                "explain",
                                "none"),
                        Arguments.of("fact-only", "一加一等于几", "等于二。", "none", "none"),
                        Arguments.of("translate-explain", "翻译 explain", "解释。", "none", "none"),
                        Arguments.of("explanation-refusal", "给我解释一下", "我现在还不知道答案。", "none", "none"),
                        Arguments.of(
                                "comfort-not-explanation",
                                "今天很难过",
                                "先别为难自己，我会陪着你。",
                                "none",
                                "none"),
                        Arguments.of(
                                "quoted-explanation",
                                "只重复这句话：因为空气会散射光",
                                "因为空气会散射光。",
                                "none",
                                "none"),
                        Arguments.of(
                                "explain-explicit-action",
                                "讲一下天为什么蓝并挥挥手",
                                "好呀，我先向你挥挥手。",
                                "none",
                                "none"))
                .filter(
                        arguments ->
                                selected == null
                                        || selected.isBlank()
                                        || arguments.get()[0].toString().matches(selected));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("greetings")
    void recognizesConversationalIntentWithoutEnactingQuotedOrExplicitActions(
            String label, String user, String assistant, String recent, String expected)
            throws Exception {
        var properties = new AvatarDecisionProperties();
        properties.setApiKey(System.getenv("DASHSCOPE_API_KEY"));
        String base = System.getenv("AVATAR_DECISION_BASE_URL");
        if (base != null && !base.isBlank()) properties.setBaseUrl(base);
        String timeout = System.getenv("AVATAR_DECISION_PROBE_TIMEOUT_MS");
        if (timeout != null) properties.setTimeoutMs(Integer.parseInt(timeout));
        var client =
                new QwenAvatarDecisionClient(
                        properties, new BailianRealtimeProperties(), new ObjectMapper(), HTTP);
        long start = System.nanoTime();
        var decision =
                client.decide(new AvatarDecisionInput(user, assistant, recent))
                        .get(6, TimeUnit.SECONDS);
        System.out.printf(
                "avatar-probe case=%s intent=%s elapsedMs=%d budgetMs=%d%n",
                label,
                decision.intent(),
                TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start),
                properties.getTimeoutMs());
        assertEquals(expected, decision.intent(), label);
    }
}
