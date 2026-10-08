package cc.shturl.wa.demo.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TaskServiceImplWeeklyAutoWinTest {

    @Test
    @DisplayName("自动胜用负的对局号计 1 点进度，不和玩家 id 冲突")
    void autoWinUsesNegativeMatchId() {
        assertThat(TaskServiceImpl.weeklyAutoWinId(1820L)).isEqualTo(-1820L);
        assertThat(TaskServiceImpl.weeklyAutoWinId(1929L)).isNegative();
    }
}
