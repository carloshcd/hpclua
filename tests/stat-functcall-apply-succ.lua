-- stat-functcall-apply-succ.lua, v1.0, 2019-08-04 22:33:23.274153288 -0300, chcd
-- Please see copyright notice in file dotests
function succ (x) 
   return x + 1
end

function apply (f, x)
   return f (x)
end

y = apply(succ, 0)
