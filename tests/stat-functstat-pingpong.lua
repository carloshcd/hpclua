-- stat-functstat-pingpong.lua, v1.0, 2019-07-30 23:25:19.803487067 -0300, chcd
-- Please see copyright notice in file dotests
function ping (x) 
   if (x <= 0) then
      return 0
   else
      return ping (x - 1)
   end
end

function pong (y)
   if (y <= 0) then
      return 1 
   else
      return pong (y - 1)
   end
end
