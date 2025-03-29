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

### Engineering Dilemma: Where to circuit break?
Do you circuit break in the controller or the service layer?
The answer to this question depends on what you are circuit breaking. 

**Typically, Your Service Layer - When You Have Downstream unstable API integration**
- Also, your controller should only worry about HTTP request/response mapping.
- Shouldn't be aware of your service layer or downstream orchestration.
- Circuit breaking is a business resilience concern to protect your service from downstream failures.
- Your service layer has access to the context: which downstreams, retries, fallbacks, metrics, etc.

**What Are You Circuit Breaking?**
- Let’s say your service integrates with a downstream API (like payment gateway, identity provider, external CRM).
- You're circuit breaking that downstream call.
- Not your own API per se, but the outgoing call from your service to the external system.

**Think of it like:**
- ❌ "This payment API keeps failing, let’s stop calling it for 60 seconds."
- ✅ _**NOT: "Let’s reject calls to our endpoint."**_

But… it does affect your API users.
Because when the breaker is open:

You might respond with fallback data.

Or HTTP 503 / 429.

Or redirect traffic (in case of HA setups).

Or just fail fast instead of hanging on retries.

**NEXT Steps**
🔍 Test Cases
📊 Exposing Resilience4j metrics via Actuator
📈 Explore more configuration options 
🧪 Add another API with different circuit breaker settings
💣 API curl on loop and watch the circuit breaker state through health API



