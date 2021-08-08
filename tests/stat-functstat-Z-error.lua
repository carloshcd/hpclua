-- stat-functstat-Z-error.lua, v1.0, 2019-08-09 20:56:13.107852888 -0300, chcd
--
local Z = 
   function (f) 
      return (function (x) 
                 return f (function (v) 
                              return x(x)(v) 
                           end) 
              end) (function (x)
                       return f (function (v)
                                    return x(x)(v)
                                 end)
                    end)
   end
