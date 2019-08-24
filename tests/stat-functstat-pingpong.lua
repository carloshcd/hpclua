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
