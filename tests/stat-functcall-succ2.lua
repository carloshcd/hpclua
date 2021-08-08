-- stat-functcall-succ2.lua, v1.0, 2019-07-30 08:10:10.523593657 -0300, chcd
-- Please see copyright notice in file dotests
function succ (x) 
   return x + 1
end
y = succ(succ (0))
