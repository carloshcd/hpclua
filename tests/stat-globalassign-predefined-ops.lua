-- stat-globalassign-predefined-ops.lua, v1.0, 2019-10-20 09:34:35.414891491 -0300, chcd
-- Please see copyright notice in file dotests
a = 1 == 2
b = 1 ~= 2
c = 1 <= 2.5
d = 1.5 >= 2
e = 1 < 2
f = 1 > 2

g = not true
h = true and false
i = true or false

j0 = # "Carlos"
k0 = "Carlos" .. "Henrique"
k1 = 1 .. "Henrique"
k2 = "Carlos" .. 2
k3 = 1 .. 2

l = -1
m = 1 + 2
n = 1 - 2.5
o = 1.5 * 2
p = 1 / 2
q = 1 ^ 2

r = 1 % 2
s = 1 // 2

t = ~1
u = 1 & 2
v = 1 | 2
x = "1" >> 2
y = 1 << "2"
