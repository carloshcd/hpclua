-- stat-functstat-swap-pair.lua, v1.0, 2019-08-06 21:57:19.933762047 -0300, chcd
-- Please see copyright notice in file dotests
function swap (x, y)
   return y, x
end

function pair (x, y) 
   return x, y 
end

function swappair (x,y)
   return swap (pair (x, y))
end
