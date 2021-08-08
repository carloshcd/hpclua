-- stat-functstat-F.lua, v1.0, 2019-08-25 09:50:05.021269241 -0300, chcd
--
local F = function (f)
             return function (n)
                       if n == 0 then 
                          return 1
                       else 
                          return n * f(n - 1)
                       end
                    end
          end
