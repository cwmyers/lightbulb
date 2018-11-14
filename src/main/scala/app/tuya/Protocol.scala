package app.tuya

import app.model.DeviceId

object Protocol {
  def createTuyaMessage(deviceId: DeviceId, timestamp: Long, payload: String): String = {
    val sequence = 0
    s"""{"data":{"devId":"${deviceId.value}","dps":$payload},"protocol":5,"s":$sequence,"t":$timestamp}"""
  }
}
