-- stat-return-sequence-adjust.lua, v1.0, 2019-10-21 21:04:48.582793611 -0300, chcd
-- Please see copyright notice in file dotests
function pair(x,y) return x,y end
function constpair () return pair("carlos",1),pair(true,nil) end
local a,b = constpair()
