-- stat-functstat-return-many-nonstrict.lua, v1.0, 2020-06-14 19:33:11.402681806 -0300, chcd
-- Please see copyright notice in file dotests
function many (x)
   if (x == 0) then 
      return nil
   elseif (x == 1) then
      return false
   elseif (x == 2) then 
      return "carlos"
   elseif (x == 3) then
      return 1
   elseif (x == 4) then
      return {}
   else 
      return (function () end)
   end
end
