function string.split(str, delimiter, maxnum, useregex)
    local plain = not useregex
    local result = {}
    local n = 0
    local p = delimiter
    local nextpos = 1
    if maxnum==nil or maxnum<1 then maxnum = 0 end
    while true do
        n = n+1
        if (maxnum>0 and maxnum<=n) then break end
        local s, e = string.find(str, p, nextpos, plain)
        if s==nil then break end
        result[n] = string.sub(str, nextpos, s-1)
        nextpos = e+1
    end
    result[n] = string.sub(str, nextpos)
    return result
end

function numToPercent(number, decimal, noSign, isNeedZero)
    decimal = decimal or 2
    noSign = (noSign ~= nil) and noSign or false
    local result = rounding(number, decimal, true, isNeedZero)
    if noSign == true then
        return (tonumber(result) * 0.01)
    end
    if noSign == false then
        return result..[[%]]
    end
end

--[[
    @desc   四舍五入
    @param  number    处理数
    @param  decimal   保留的小数位数(默认保留0位)
    @param  isPercent 是否返回百分比的值(默认false)
    @return 数值字符串
--]]
function rounding(number, decimal, isPercent, isNeedZero)
    return _transNumber(number, 5, decimal, isPercent, isNeedZero)
end

--[[
    @desc   向上舍入
    @param  number    处理数
    @param  decimal   保留的小数位数(默认保留0位)
    @param  isPercent 是否返回百分比的值(默认false)
    @return 数值字符串
--]]
function ceil(number, decimal, isPercent, isNeedZero)
    return _transNumber(number, 1, decimal, isPercent, isNeedZero)
end

--[[
    @desc   向下舍入
    @param  number    处理数
    @param  decimal   保留的小数位数(默认保留0位)
    @param  isPercent 是否返回百分比的值(默认false)
    @return 数值字符串
--]]
function floor(number, decimal, isPercent, isNeedZero)
    return _transNumber(number, 10, decimal, isPercent, isNeedZero)
end

--[[
    @desc   toint
    @param  number    处理数
    @return 转数字
--]]
function toint(number)
    if number == nil then return 0 end
    if number == '' then return 0 end
    local num = tonumber(number) 
    if num == nil then return 0 end
    return num < 0 and math.ceil(num) or math.floor(num)
end

--[[
    @desc   (N-1)舍N入
    @param  number    处理数
    @param  n         舍入里需要入的N数(0~10)(默认四舍五入)
    @param  decimal   保留的小数位数(默认保留0位)
    @param  isPercent 是否返回百分比的值(默认false)
    @return 数值字符串
--]]
function _transNumber(number, n, decimal, isPercent, isNeedZero)
    local num = number and tonumber(number) or 0
    local dec = decimal or 0
    local d = math.pow(10, dec)
    n = n or 5
    num = num * d
    num = math.floor(num + (0.1 * (10 - n)))
    num = num / d
    local format = [[%.]]..dec.."d"
    num = tonumber(string.format(format, num))
    local t = {}
    local zero = 0
    if isPercent then
        num = num * 100
        t = string.split(num, ".")
        if t[2] then
            zero = math.max(
                (math.max((dec - 2), 0) - string.len(t[2])), 
                0)
        else
            zero = math.max((dec - 2), 0)
        end
    else
        t = string.split(num, ".")
        if t[2] then
            zero = math.max(
                (dec - string.len(t[2])), 
                0)
        else
            zero = dec
        end
    end
    if isNeedZero == false then
        zero = 0 
    end
    if t[2] or zero > 0 then
        t[2] = t[2] or ""
        for i = 1, zero do
            t[2] = t[2].."0"
        end
        return t[1].."."..t[2]
    else
        return t[1]
    end
end


function test()
    local num1 = 12
    local num2 = 12.123
    local num3 = 13.5
    local num4 = 22.380922
    print("num4", num4 * 2)
    return tonumber(num4) * tonumber(2)
end

local result = test()
print("result:" .. result)
print("result x 2:", result * tonumber(2))

return result