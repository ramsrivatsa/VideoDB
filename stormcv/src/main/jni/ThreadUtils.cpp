#include "ThreadUtils.h"

#include <stdexcept>

#include <errno.h>
#include <sched.h>
#include <string.h>
#include <unistd.h>
#include <sys/syscall.h>
#include <sys/types.h>

namespace ucw {

void setPriority(int priority)
{
    static char buf[512];
    sched_param param;
    param.sched_priority = priority;
    pid_t tid;

    tid = syscall(SYS_gettid);

    if (sched_setscheduler(tid, SCHED_RR, &param) == -1) {
        int errsv = errno;
        const char *msg = strerror_r(errsv, buf, 512);
        throw runtime_error(msg);
    }
}

long getCurrentTid()
{
    pid_t tid = syscall(SYS_gettid);
    return tid;
}

} // namespace ucw