## Trying Reslience4j Reactive

### 1. CLOSED State
1. Everything looks fine. Let requests flow.
2. All calls are allowed through.
3. Failures are recorded.
4. If failure rate exceeds a threshold (e.g., 50%) over a sliding window, the circuit transitions to OPEN.

### 2. OPEN State
1. Too many failures. Stop! Don’t call the service.
2. No calls are allowed through — they fail fast with a CallNotPermittedException.
3. After a configurable waitDurationInOpenState, the breaker transitions to HALF_OPEN to test recovery.

### HALF_OPEN State 
1. Let me test a few requests. Maybe the service is back? 
2. Allows only a limited number of trial calls. 
3. If a certain number of successes occur → transitions back to CLOSED. 
4. If any trial call fails → goes back to OPEN.

**Visual Representation of Circuit Breaker States:**
![cb_visual_summary.png](cb_visual_summary.png)

**These are special, usually manual states:**
1. DISABLED: No protection, breaker does nothing.
2. FORCED_OPEN: Always blocks calls, like OPEN but never transitions.
3. FORCED_CLOSED: Always allows calls, like CLOSED but never transitions.