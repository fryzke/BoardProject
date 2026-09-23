-- KEYS[1]: 토큰 버킷 키 (예: "ratelimit:bucket:user_123")
-- ARGV[1]: 버킷 최대 용량 (Max Capacity, 예: 10)
-- ARGV[2]: 초당 충전 속도 (Refill Rate, 예: 2)
-- ARGV[3]: 현재 타임스탬프 (초 단위)
-- ARGV[4]: 소비할 토큰 수 (일반적으로 1)

local key = KEYS[1]
local max_capacity = tonumber(ARGV[1])
local refill_rate = tonumber(ARGV[2])
local now = tonumber(ARGV[3])
local requested = tonumber(ARGV[4])

-- 기존 데이터 불러오기
local data = redis.call('HMGET', key, 'tokens', 'last_updated')
local tokens = tonumber(data[1])
local last_updated = tonumber(data[2])

if tokens == nil then
    -- 최초 요청 시 버킷을 가득 채운 상태로 초기화
    tokens = max_capacity
    last_updated = now
else
    -- 1. 경과 시간에 따른 토큰 충전 계산
    local elapsed = now - last_updated
    if elapsed > 0 then
        local refill = elapsed * refill_rate
        tokens = math.min(max_capacity, tokens + refill)
        last_updated = now
    end
end

-- 2. 토큰이 충분한지 확인
-- 매초 타이머로 토큰을 충전하면 부하가 크므로, 요청이 들어온 시점에 수식으로 충전량을 계산
if tokens >= requested then
    tokens = tokens - requested
    redis.call('HMSET', key, 'tokens', tokens, 'last_updated', last_updated)
    -- 여유 있게 키 만료시간 설정 (예: 1시간 후 자동 삭제)
    redis.call('EXPIRE', key, 3600)
    return 1 -- 허용
else
    -- 잔여 토큰이 부족하면 충전된 상태만 업데이트하고 차단
    redis.call('HMSET', key, 'tokens', tokens, 'last_updated', last_updated)
    return 0 -- 차단
end
