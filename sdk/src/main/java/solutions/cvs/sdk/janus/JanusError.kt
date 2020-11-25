package solutions.cvs.sdk.janus

import solutions.cvs.sdk.SDKError

class JanusError(val code: Int, reason: String) : SDKError(reason)