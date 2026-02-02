package com.sight.service

import org.springframework.stereotype.Service
import java.time.LocalTime
import kotlin.random.Random

@Service
class TipService {
    private val tips =
        listOf(
            "그룹은 언제든지 만들 수 있어요. 심지어 방학에도요!",
            "그룹 활동 인원이 잘 안 구해진다면, 디스코드의 '같이해요' 채널에서 모집해봐요!",
            "교육 활동은 매 학기 초에 개설돼요.",
            "매 학기 사이트에서 info21 인증을 해야 해요. 휴학 기간에도요!",
            "군 입대를 앞두고 있다고요? 운영진에게 '회원 정지'를 요청하세요!",
            "졸업을 하셨다면 졸업 신고를 해주세요!",
            "졸업을 하더라도 명예 회원으로 계속 활동할 수 있어요!",
            "세미나에서 발표를 해야 한 학기 활동을 인정받을 수 있어요!",
            "'장부' 메뉴에서 회비 사용 현황을 확인할 수 있어요!",
            "운영진에 관심이 있나요? 기존 운영진에게 문의해주세요!",
            "쿠러그 공식 이메일은 we_are@khlug.org입니다.",
            "디스코드는 사이트와 연동을 한 뒤부터 활동할 수 있어요!",
            "휴학이나 복학, 전과, 다전공한 뒤에도 info21 인증을 부탁해요!",
        )

    fun getRandomTip(): String = "TIP: ${tips.random()}"

    fun getTimeBasedMention(): String {
        val hour = LocalTime.now().hour
        return when (hour) {
            in 0..5 -> "오늘도 밤을 새나요?"
            in 6..10 -> "좋은 아침입니다!"
            in 11..15 -> "맑은 하루입니다."
            in 16..20 -> "창 밖의 달이 보이나요?"
            else -> "오늘도 하루가 끝나갑니다."
        }
    }

    fun getRandomMention(): String {
        return if (Random.nextDouble() < 0.6) {
            getRandomTip()
        } else {
            getTimeBasedMention()
        }
    }
}
