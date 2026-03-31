package com.blackbox.domain.score.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScoreScheduler {

    private final ScoreEngine scoreEngine;

    /** 30분마다 전체 프로젝트 점수 재계산 */
    @Scheduled(fixedDelay = 1_800_000)
    public void recalculateAll() {
        log.info("Scheduled score recalculation started");
        scoreEngine.calculateAll();
        log.info("Scheduled score recalculation completed");
    }
}
