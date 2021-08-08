-- stat-functstat-return-okBool-nonstrict.lua, v1.0, 2020-06-09 16:16:39.781877317 -0300, chcd
-- Please see copyright notice in file dotests
function okBool (x)
   if (x == true or x == false) then 
      return "ok"
   else
      return nil
   end
end
