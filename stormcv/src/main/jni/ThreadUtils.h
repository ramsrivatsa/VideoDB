##ifndef THREADUTILS_H
#define THREADUTILS_H

namespace ucw {

void setPriority(int priority);

long getCurrentTid();

}
#endif // THREADUTILS_H