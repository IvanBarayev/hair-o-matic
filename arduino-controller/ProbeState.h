#ifndef PROBESTATE_INCLUDED
#define PROBESTATE_INCLUDED
#include "IOUtils.h"

class ProbeState {
private:
    int killCount = 0;
    int lifeTimeKillCount = 0;

    long probeActiveStartTime = 0;
    bool isProbeActive = false;
    int targetMicroAmps = 550;
    bool isRefreshNeeded = true;

public:
    void initialize() {
        lifeTimeKillCount = IOUtils::readEepromInt(0);

        targetMicroAmps = IOUtils::readEepromInt(4);
        if (targetMicroAmps < 1 || targetMicroAmps > 1000)
            targetMicroAmps = 500;
    }

    void decreaseTargetCurrent() {
        if (targetMicroAmps <= 100)
            return;
        
        targetMicroAmps -= 50;
        IOUtils::saveEepromInt(targetMicroAmps, 4);
        isRefreshNeeded = true;
    }

    void increaseTargetCurrent() {
        if (targetMicroAmps >= 1200)
            return;

        targetMicroAmps += 50;
        IOUtils::saveEepromInt(targetMicroAmps, 4);
        isRefreshNeeded = true;
    }

    long getActiveTime() {
        if (isProbeActive)
            return (millis() - probeActiveStartTime) / 1000;

        return 0;
    }

    double getPreciseActiveTime() {
        if (isProbeActive)
            return (millis() - probeActiveStartTime) / 1000.0;

        return 0;
    }

    int getKillCount() { return killCount; }
    void incrementKillCount() { killCount ++;}

    int getLifeTimeCount() { return lifeTimeKillCount; }
    void incrementLifeTimeCount() {
        lifeTimeKillCount++;
        IOUtils::saveEepromInt(lifeTimeKillCount, 0);
    }

    void setTargetMicroAmps(int target) { targetMicroAmps = target; }
    int getTargetMicroAmps() { return targetMicroAmps;}

    void setIsProbeActive(bool active) {
        if (!isProbeActive && active)
            probeActiveStartTime = millis();

        isProbeActive = active;
    }
    bool getIsProbeActive() { return isProbeActive; }

    void setIsRefreshNeeded(bool active) {isRefreshNeeded = active; }
    bool getIsRefreshNeeded() { return isRefreshNeeded; }

    bool isFirstLoop = true;
    double lastInputVoltage = 1;
    double resistance = 1;
};

#endif
