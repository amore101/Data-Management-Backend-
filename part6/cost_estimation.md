R.a > 200 (clustered index scan)

X * B(R) = (250-200)/(250-150) * 1000 = 500

R.a = S.a (block-at-a-time nested loop join)

B(R') + B(R') * B(S) = 500 + 500 * 2000 = 1000500

S.b = U.b (unclustered nested loop join)

B(RS) + T(RS) * (X * T(U)) = 1000500 + 4 * 10^8 * (10^4 * 1/250) 
= 1.6 * 10^10 + 1.0005 * 10^6 = 16000000000 + 1000500 = 16001000500