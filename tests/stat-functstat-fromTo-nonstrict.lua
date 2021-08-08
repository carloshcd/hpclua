-- stat-functstat-fromTo-nonstrict.lua, v1.0, 2019-08-09 20:48:22.712242964 -0300, chcd
--
function fromTo (n, m)
   return function ()
      if n > m then
         return nil
      else
         n = n+1
         return n-1
      end
   end
end
