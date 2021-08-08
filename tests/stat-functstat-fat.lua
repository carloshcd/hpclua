-- stat-functstat-fat.lua, v1.0, 2019-07-30 23:17:09.406668802 -0300, chcd
-- Please see copyright notice in file dotests
function fat (x)
   if (x == 0) then
      return 1
   else 
      return x * fat (x - 1) 
   end
end
