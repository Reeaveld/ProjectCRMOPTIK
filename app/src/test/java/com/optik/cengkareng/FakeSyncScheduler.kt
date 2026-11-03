package com.optik.cengkareng

import com.optik.cengkareng.core.sync.SyncScheduler

class FakeSyncScheduler : SyncScheduler {
    var pushRequests = 0
        private set
    var periodicRequests = 0
        private set

    override fun schedulePush() {
        pushRequests++
    }

    override fun ensurePeriodic() {
        periodicRequests++
    }
}
