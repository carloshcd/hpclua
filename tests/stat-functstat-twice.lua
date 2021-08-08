-- stat-functstat-twice.lua, v1.0, 2020-07-19 21:34:34.630984105 -0300, chcd
-- Please see copyright notice in file dotests
function twice (f)
   return function (x)
             return f(f(x))
          end
end
