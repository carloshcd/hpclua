-- stat-functstat-succ2.lua, v1.0, 2019-07-28 13:00:01.067547132 -0300, chcd
-- Please see copyright notice in file dotests
function succ (x) 
   return x + 1
end

function succ2 (x)
   return succ (succ (x))
end
