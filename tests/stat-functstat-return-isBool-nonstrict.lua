-- stat-functstat-return-isBool-nonstrict.lua, v1.0, 2021-01-30 15:10:14.738369066 -0300, chcd
-- Please see copyright notice in file dotests
function isBool (x)
   if (x == true or x == false) then 
      return true
   else
      return nil
   end
end
