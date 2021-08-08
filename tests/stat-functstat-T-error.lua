-- stat-functstat-T-error.lua, v1.0, 2019-08-09 20:56:13.107852888 -0300, chcd
-- Please see copyright notice in file dotests
local T = 
      (function (x,y) 
          return y(x(x)(y)) 
       end) (function (x,y)
                return y(x(x)(y))
             end)
