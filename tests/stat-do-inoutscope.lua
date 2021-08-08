-- stat-do-inoutscope.lua, v1.0, 2021-08-08 19:21:00.734651708 -0300, chcd
-- Please see copyright notice in file dotests
x = 1
do 
   local x = "a"
end
x = x + 1
