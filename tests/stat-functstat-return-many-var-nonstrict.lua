-- stat-functstat-return-many-var-nonstrict.lua, v1.0, 2020-07-06 11:20:53.442841135 -0300, chcd
-- Please see copyright notice in file dotests
function bottom (x)
   if (x == 0) then 
      return nil
   elseif (x == 1) then
      return false
   else 
      return x
   end
end
