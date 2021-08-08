-- stat-functcall-sequence-adjust.lua, v1.0, 2019-10-22 07:48:41.506847134 -0300, chcd
-- Please see copyright notice in file dotests
function pair(x,y) return x,y end
a, b = pair(pair(nil,true), pair(1,"carlos"))
