-- stat-forgen-nonstrict.lua, v1.0, 2020-11-09 15:59:38.449618221 -0300, chcd
-- Please see copyright notice in file dotests
function fromTo (n, m)
   return function ()
      if n > m then
         return nil
      else
         n = n + 1
         return n - 1
      end
   end
end

for x in fromTo(10, 12) do
   local y = x
end

