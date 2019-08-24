function fat (x)
   if (x == 0) then
      return 1
   else 
      return x * fat (x - 1) 
   end
end
