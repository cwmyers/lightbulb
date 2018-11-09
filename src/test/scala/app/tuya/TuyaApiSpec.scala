package app.tuya

class TuyaApiSpec extends org.specs2.mutable.Specification {
  val message = """2.109e68d113f7b2a4fq17L5OX/6yNg58mGTxKVSKJqPacAfFzbMami2I3IdW3znL5S/35LZ1hfV1v2AhW4o1vjq+EFzyYRZYTRjvppkBWHoLmW0JyY9ckH3xUlFdG42BbaBCA6I89dFC1LlXaB6mXpL1zx4NnD77Cpz7uoeIb+WaBOQX1j0NITrBSQLf1C8hFOTqGopRFHRJRK9zd4YLfyRoQx2y57OSAU6oXU2iIeO2kFQYhFHMPtR1o8aRlxAxtyba/5fdDYREnetSSt2ESp4H9MhDC1B7/ov4aXYMJQFIXuQ4/MRxAaIK1YxHY4LLGUal9DczFYbvLm43xgQ/NUA/yyX+L0cEQ3+/bjxIrfkb3U/kRZFjazcHcfcwKqqeVHTOE5CwsmuulIahe97YzaIGI9e7tDqceoJfNvCA=="""

  "process method" should {
    "successfully decode message with valid key and message" in {
      TuyaApi.process("cb67818210934165", message) must beRight
    }

    "fail to decode message with bad base64 values" in {
      val badMessage = """2.109e68d113f7b2a4fq17L5OX/6yNg58mGTxKVSfsdjfasdfu9aufsufznL5S/35LZ1hfV1v2AhW4o1vjq+EFzyYRZYTRjvppkBWHoLmW0JyY9ckH3xUlFdG42BbaBCA6I89dFC1LlXaB6mXpL1zx4NnD77Cpz7uoeIb+WaBOQX1j0NITrBSQLf1C8hFOTqGopRFHRJRK9zd4YLfyRoQx2y57OSAU6oXU2iIeO2kFQYhFHMPtR1o8aRlxAxtyba/5fdDYREnetSSt2ESp4H9MhDC1B7/ov4aXYMJQFIXuQ4/MRxAaIK1YxHY4LLGUal9DczFYbvLm43xgQ/NUA/yyX+L0cEQ3+/bjxIrfkb3U/kRZFjazcHcfcwKqqeVHTOE5CwsmuulIahe97YzaIGI9e7tDqceoJfNvCA=="""
      TuyaApi.process("cb67818210934165", badMessage) must beLeft
    }
    "fail to decode empty message" in {
      TuyaApi.process("cb67818210934165", "") must beLeft
    }

    "fail to decode message with bad key" in {
      TuyaApi.process("adfasdfasdf", message) must beLeft
    }
    "fail to decode message with empty key" in {
      TuyaApi.process("", message) must beLeft
    }
  }

}
