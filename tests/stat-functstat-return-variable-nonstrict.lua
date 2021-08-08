-- stat-functstat-return-variable-nonstrict.lua, v1.0, 2020-06-09 16:08:23.265987609 -0300, chcd
-- Please see copyright notice in file dotests
function variable (x)
   if (x) then 
      return x
   else
      return nil
   end
end
