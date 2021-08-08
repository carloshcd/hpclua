-- stat-functstat-S.lua, v1.0, 2020-07-19 22:11:14.961198451 -0300, chcd
-- Please see copyright notice in file dotests
local S = 
   function (f)
      return function (g) 
                return function (h)
                          return f(h)(g(h))
                       end
             end
   end
