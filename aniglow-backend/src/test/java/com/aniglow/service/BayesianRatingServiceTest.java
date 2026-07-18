package com.aniglow.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DisplayName("贝叶斯平均分算法")
class BayesianRatingServiceTest {

    private final BayesianRatingService service = new BayesianRatingService(null, null);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "globalMean", 7.0);
        ReflectionTestUtils.setField(service, "confidenceWeight", 25.0);
    }

    @Nested
    @DisplayName("正常计算")
    class NormalCalculation {

        @Test
        @DisplayName("评分人数多时偏向自身评分")
        void highRatingCountLeansTowardOwnScore() {
            // R=8.5, v=1000, m=25, C=7.0
            // weighted = (1000/1025)*8.5 + (25/1025)*7.0
            double result = service.calculateBayesianAverage(8.5, 1000L);

            assertThat(result).isCloseTo(8.5, within(0.2));
        }

        @Test
        @DisplayName("评分人数等于置信权重时平滑过渡")
        void ratingCountEqualsConfidenceWeight() {
            // R=9.0, v=25, m=25, C=7.0
            // weighted = (25/50)*9.0 + (25/50)*7.0 = 8.0
            double result = service.calculateBayesianAverage(9.0, 25L);

            assertThat(result).isEqualTo(8.0);
        }

        @ParameterizedTest
        @CsvSource({
            "9.0, 10,    7.57",   // 极少评分 → 向全局平均分回归
            "9.0, 50,    8.33",   // 等于 m → 接近平均值
            "9.0, 500,   8.90",   // 多评分 → 接近自身评分
            "5.0, 5,     6.67",   // 低分小众 → 向上回归
            "5.0, 100,   5.40",   // 低分大众 → 接近自身低分
        })
        @DisplayName("贝叶斯回归效果验证")
        void bayesianRegression(double meanRating, long ratingCount, double expected) {
            double result = service.calculateBayesianAverage(meanRating, ratingCount);

            assertThat(result).isCloseTo(expected, within(0.01));
        }
    }

    @Nested
    @DisplayName("边界与异常输入")
    class EdgeCases {

        @Test
        @DisplayName("评分为null时返回全局平均分")
        void nullMeanRatingReturnsGlobalMean() {
            double result = service.calculateBayesianAverage(null, 100L);

            assertThat(result).isEqualTo(7.0);
        }

        @Test
        @DisplayName("评分人数为null时返回全局平均分")
        void nullRatingCountReturnsGlobalMean() {
            double result = service.calculateBayesianAverage(8.5, null);

            assertThat(result).isEqualTo(7.0);
        }

        @Test
        @DisplayName("评分人数为0时返回全局平均分")
        void zeroRatingCountReturnsGlobalMean() {
            double result = service.calculateBayesianAverage(9.0, 0L);

            assertThat(result).isEqualTo(7.0);
        }

        @Test
        @DisplayName("评分人数极大时结果接近自身评分")
        void veryLargeRatingCountApproachesOwnScore() {
            double result = service.calculateBayesianAverage(9.5, 1_000_000L);

            assertThat(result).isCloseTo(9.5, within(0.001));
        }

        @Test
        @DisplayName("结果为两位小数精度")
        void precisionIsTwoDecimalPlaces() {
            double result = service.calculateBayesianAverage(8.123, 30L);

            String asString = String.valueOf(result);
            int decimalPlaces = asString.contains(".")
                    ? asString.length() - asString.indexOf('.') - 1
                    : 0;

            // 最多两位小数，或者整数
            assertThat(decimalPlaces).isLessThanOrEqualTo(2);
        }
    }
}
