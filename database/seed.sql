-- =============================================================================
-- WayPoint - Dados iniciais de transporte publico (seed.sql)
-- Extraido exclusivamente do dump oficial do PostgreSQL.
-- Contem apenas linhas e paradas, preservando IDs, ordem, sentidos e coordenadas.
-- =============================================================================

SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;

BEGIN;

-- -----------------------------------------------------------------------------
-- Linhas de transporte publico
-- -----------------------------------------------------------------------------
INSERT INTO public.linha (id, nome, status, codigo) OVERRIDING SYSTEM VALUE VALUES (1, 'Vila Brasil', 'NORMAL', '101');
INSERT INTO public.linha (id, nome, status, codigo) OVERRIDING SYSTEM VALUE VALUES (2, 'FATEC/AME', 'NORMAL', '102');
INSERT INTO public.linha (id, nome, status, codigo) OVERRIDING SYSTEM VALUE VALUES (3, 'Vila Musa', 'NORMAL', '103');
INSERT INTO public.linha (id, nome, status, codigo) OVERRIDING SYSTEM VALUE VALUES (4, 'Distrito Industrial', 'NORMAL', '104');

-- -----------------------------------------------------------------------------
-- Paradas das linhas (preservando IDs, ordem de percurso, sentido e coordenadas)
-- -----------------------------------------------------------------------------

-- Linha 101 - Vila Brasil (IDA)
INSERT INTO public.parada (id, linha_id, nome, latitude, longitude, ordem, sentido) OVERRIDING SYSTEM VALUE VALUES (1, 1, 'Terminal Centro', -22.97627412, -49.86949015, 1, 'IDA');
INSERT INTO public.parada (id, linha_id, nome, latitude, longitude, ordem, sentido) OVERRIDING SYSTEM VALUE VALUES (2, 1, 'ConstruCasa', -22.97618376, -49.86686427, 2, 'IDA');
INSERT INTO public.parada (id, linha_id, nome, latitude, longitude, ordem, sentido) OVERRIDING SYSTEM VALUE VALUES (3, 1, 'Magic Tintas', -22.97387998, -49.86700949, 3, 'IDA');
INSERT INTO public.parada (id, linha_id, nome, latitude, longitude, ordem, sentido) OVERRIDING SYSTEM VALUE VALUES (4, 1, 'Caló', -22.97086430, -49.86735032, 4, 'IDA');
INSERT INTO public.parada (id, linha_id, nome, latitude, longitude, ordem, sentido) OVERRIDING SYSTEM VALUE VALUES (5, 1, 'Entrada Rodoviária', -22.96738942, -49.86767359, 5, 'IDA');
INSERT INTO public.parada (id, linha_id, nome, latitude, longitude, ordem, sentido) OVERRIDING SYSTEM VALUE VALUES (6, 1, 'Tênis Clube', -22.96704035, -49.86403915, 6, 'IDA');
INSERT INTO public.parada (id, linha_id, nome, latitude, longitude, ordem, sentido) OVERRIDING SYSTEM VALUE VALUES (7, 1, 'Transportadora Floresta', -22.96409792, -49.86483228, 7, 'IDA');
INSERT INTO public.parada (id, linha_id, nome, latitude, longitude, ordem, sentido) OVERRIDING SYSTEM VALUE VALUES (8, 1, 'Coelho Style', -22.96172880, -49.86647806, 8, 'IDA');
INSERT INTO public.parada (id, linha_id, nome, latitude, longitude, ordem, sentido) OVERRIDING SYSTEM VALUE VALUES (9, 1, 'Entrada da Vila Brasil', -22.96099770, -49.86721342, 9, 'IDA');
INSERT INTO public.parada (id, linha_id, nome, latitude, longitude, ordem, sentido) OVERRIDING SYSTEM VALUE VALUES (10, 1, 'Vila Brasil', -22.95798046, -49.86776815, 10, 'IDA');

-- Linha 101 - Vila Brasil (VOLTA)
INSERT INTO public.parada (id, linha_id, nome, latitude, longitude, ordem, sentido) OVERRIDING SYSTEM VALUE VALUES (11, 1, 'Vila Brasil', -22.95798046, -49.86776815, 1, 'VOLTA');
INSERT INTO public.parada (id, linha_id, nome, latitude, longitude, ordem, sentido) OVERRIDING SYSTEM VALUE VALUES (12, 1, 'Takinho Bar', -22.95968973, -49.86727805, 2, 'VOLTA');
INSERT INTO public.parada (id, linha_id, nome, latitude, longitude, ordem, sentido) OVERRIDING SYSTEM VALUE VALUES (82, 1, 'Mário Pastel', -22.96099770, -49.86721342, 3, 'VOLTA');
INSERT INTO public.parada (id, linha_id, nome, latitude, longitude, ordem, sentido) OVERRIDING SYSTEM VALUE VALUES (13, 1, 'Rodoviária', -22.96757284, -49.86668135, 4, 'VOLTA');
INSERT INTO public.parada (id, linha_id, nome, latitude, longitude, ordem, sentido) OVERRIDING SYSTEM VALUE VALUES (14, 1, 'Virginia', -22.97047019, -49.86855919, 5, 'VOLTA');
INSERT INTO public.parada (id, linha_id, nome, latitude, longitude, ordem, sentido) OVERRIDING SYSTEM VALUE VALUES (15, 1, 'Point Esfihas', -22.97311728, -49.86826845, 6, 'VOLTA');
INSERT INTO public.parada (id, linha_id, nome, latitude, longitude, ordem, sentido) OVERRIDING SYSTEM VALUE VALUES (16, 1, 'Terminal Centro', -22.97627412, -49.86949015, 7, 'VOLTA');

-- Linha 102 - FATEC/AME (IDA)
INSERT INTO public.parada (id, linha_id, nome, latitude, longitude, ordem, sentido) OVERRIDING SYSTEM VALUE VALUES (17, 2, 'Terminal Centro', -22.97627412, -49.86949015, 1, 'IDA');
INSERT INTO public.parada (id, linha_id, nome, latitude, longitude, ordem, sentido) OVERRIDING SYSTEM VALUE VALUES (18, 2, 'ConstruCasa', -22.97618376, -49.86686427, 2, 'IDA');
INSERT INTO public.parada (id, linha_id, nome, latitude, longitude, ordem, sentido) OVERRIDING SYSTEM VALUE VALUES (19, 2, 'Specialista Tintas', -22.97487206, -49.86850319, 3, 'IDA');
INSERT INTO public.parada (id, linha_id, nome, latitude, longitude, ordem, sentido) OVERRIDING SYSTEM VALUE VALUES (20, 2, 'Associação Mirim', -22.97433864, -49.87112310, 4, 'IDA');
INSERT INTO public.parada (id, linha_id, nome, latitude, longitude, ordem, sentido) OVERRIDING SYSTEM VALUE VALUES (21, 2, 'Assembleia de Deus', -22.97206916, -49.87132808, 5, 'IDA');
INSERT INTO public.parada (id, linha_id, nome, latitude, longitude, ordem, sentido) OVERRIDING SYSTEM VALUE VALUES (22, 2, 'R. Barão do Rio Branco', -22.97171818, -49.87408666, 6, 'IDA');
INSERT INTO public.parada (id, linha_id, nome, latitude, longitude, ordem, sentido) OVERRIDING SYSTEM VALUE VALUES (23, 2, 'R Vitório Christoni', -22.96789532, -49.87902725, 7, 'IDA');
INSERT INTO public.parada (id, linha_id, nome, latitude, longitude, ordem, sentido) OVERRIDING SYSTEM VALUE VALUES (24, 2, 'UPA', -22.96174089, -49.88213223, 8, 'IDA');
INSERT INTO public.parada (id, linha_id, nome, latitude, longitude, ordem, sentido) OVERRIDING SYSTEM VALUE VALUES (25, 2, 'Ambulatório Santa Casa', -22.96233829, -49.88845785, 9, 'IDA');
INSERT INTO public.parada (id, linha_id, nome, latitude, longitude, ordem, sentido) OVERRIDING SYSTEM VALUE VALUES (26, 2, 'Av. Vitalina Marcusso', -22.95891679, -49.89277120, 10, 'IDA');
INSERT INTO public.parada (id, linha_id, nome, latitude, longitude, ordem, sentido) OVERRIDING SYSTEM VALUE VALUES (27, 2, 'FATEC', -22.95080307, -49.89686015, 11, 'IDA');
INSERT INTO public.parada (id, linha_id, nome, latitude, longitude, ordem, sentido) OVERRIDING SYSTEM VALUE VALUES (28, 2, 'AME', -22.94883825, -49.89770046, 12, 'IDA');

-- Linha 102 - FATEC/AME (VOLTA)
INSERT INTO public.parada (id, linha_id, nome, latitude, longitude, ordem, sentido) OVERRIDING SYSTEM VALUE VALUES (83, 2, 'AME', -22.94883825, -49.89770046, 1, 'VOLTA');
INSERT INTO public.parada (id, linha_id, nome, latitude, longitude, ordem, sentido) OVERRIDING SYSTEM VALUE VALUES (29, 2, 'SESI', -22.96234139, -49.88819551, 2, 'VOLTA');
INSERT INTO public.parada (id, linha_id, nome, latitude, longitude, ordem, sentido) OVERRIDING SYSTEM VALUE VALUES (30, 2, 'Maitan', -22.95957398, -49.88296106, 3, 'VOLTA');
INSERT INTO public.parada (id, linha_id, nome, latitude, longitude, ordem, sentido) OVERRIDING SYSTEM VALUE VALUES (31, 2, 'UPA', -22.96174089, -49.88213223, 4, 'VOLTA');
INSERT INTO public.parada (id, linha_id, nome, latitude, longitude, ordem, sentido) OVERRIDING SYSTEM VALUE VALUES (32, 2, 'Restaurante Vila Beer', -22.96398286, -49.88220069, 5, 'VOLTA');
INSERT INTO public.parada (id, linha_id, nome, latitude, longitude, ordem, sentido) OVERRIDING SYSTEM VALUE VALUES (33, 2, 'R Vitório Christoni', -22.96789532, -49.87902725, 6, 'VOLTA');
INSERT INTO public.parada (id, linha_id, nome, latitude, longitude, ordem, sentido) OVERRIDING SYSTEM VALUE VALUES (34, 2, 'Tapeçaria Avenida', -22.97028580, -49.87655917, 7, 'VOLTA');
INSERT INTO public.parada (id, linha_id, nome, latitude, longitude, ordem, sentido) OVERRIDING SYSTEM VALUE VALUES (35, 2, 'Espanhol Industria Mêcanica', -22.97521194, -49.87150057, 8, 'VOLTA');
INSERT INTO public.parada (id, linha_id, nome, latitude, longitude, ordem, sentido) OVERRIDING SYSTEM VALUE VALUES (36, 2, 'Av Jacinto Ferriera de Sá', -22.97493686, -49.86831298, 9, 'VOLTA');
INSERT INTO public.parada (id, linha_id, nome, latitude, longitude, ordem, sentido) OVERRIDING SYSTEM VALUE VALUES (37, 2, 'Terminal Centro', -22.97627412, -49.86949015, 10, 'VOLTA');

-- Linha 103 - Vila Musa (IDA)
INSERT INTO public.parada (id, linha_id, nome, latitude, longitude, ordem, sentido) OVERRIDING SYSTEM VALUE VALUES (38, 3, 'Terminal Centro', -22.97627412, -49.86949015, 1, 'IDA');
INSERT INTO public.parada (id, linha_id, nome, latitude, longitude, ordem, sentido) OVERRIDING SYSTEM VALUE VALUES (39, 3, 'Banco Caixa', -22.97903916, -49.86808835, 2, 'IDA');
INSERT INTO public.parada (id, linha_id, nome, latitude, longitude, ordem, sentido) OVERRIDING SYSTEM VALUE VALUES (40, 3, 'Jardim Éden', -22.98365297, -49.86764284, 3, 'IDA');
INSERT INTO public.parada (id, linha_id, nome, latitude, longitude, ordem, sentido) OVERRIDING SYSTEM VALUE VALUES (41, 3, 'AutoPark', -22.98758460, -49.86819254, 4, 'IDA');
INSERT INTO public.parada (id, linha_id, nome, latitude, longitude, ordem, sentido) OVERRIDING SYSTEM VALUE VALUES (42, 3, 'Yabadu Pizzaria', -22.99047109, -49.87600434, 5, 'IDA');
INSERT INTO public.parada (id, linha_id, nome, latitude, longitude, ordem, sentido) OVERRIDING SYSTEM VALUE VALUES (43, 3, 'Rua Padre Rui Cândido da Silva', -22.99179689, -49.87788933, 6, 'IDA');
INSERT INTO public.parada (id, linha_id, nome, latitude, longitude, ordem, sentido) OVERRIDING SYSTEM VALUE VALUES (44, 3, 'Paróquia Santo Antônio', -22.99491336, -49.88042100, 7, 'IDA');
INSERT INTO public.parada (id, linha_id, nome, latitude, longitude, ordem, sentido) OVERRIDING SYSTEM VALUE VALUES (45, 3, 'R. Antônio Bertagnoli', -23.00009629, -49.88210689, 8, 'IDA');
INSERT INTO public.parada (id, linha_id, nome, latitude, longitude, ordem, sentido) OVERRIDING SYSTEM VALUE VALUES (46, 3, 'Assembléia De Deus Ministério Nova Aliança', -23.00206080, -49.88348572, 9, 'IDA');
INSERT INTO public.parada (id, linha_id, nome, latitude, longitude, ordem, sentido) OVERRIDING SYSTEM VALUE VALUES (47, 3, 'SuperMercado São Judas Tadeu', -23.00681767, -49.88710642, 10, 'IDA');
INSERT INTO public.parada (id, linha_id, nome, latitude, longitude, ordem, sentido) OVERRIDING SYSTEM VALUE VALUES (48, 3, 'R. Achiles Zamboni', -23.01286677, -49.89947906, 11, 'IDA');

-- Linha 103 - Vila Musa (VOLTA)
INSERT INTO public.parada (id, linha_id, nome, latitude, longitude, ordem, sentido) OVERRIDING SYSTEM VALUE VALUES (84, 3, 'R. Achiles Zamboni', -23.01286677, -49.89947906, 1, 'VOLTA');
INSERT INTO public.parada (id, linha_id, nome, latitude, longitude, ordem, sentido) OVERRIDING SYSTEM VALUE VALUES (49, 3, 'R. Cap. N. Marcelino da Silva', -23.01332217, -49.89716340, 2, 'VOLTA');
INSERT INTO public.parada (id, linha_id, nome, latitude, longitude, ordem, sentido) OVERRIDING SYSTEM VALUE VALUES (50, 3, 'SuperMercado São Judas Tadeu', -23.00681767, -49.88710642, 3, 'VOLTA');
INSERT INTO public.parada (id, linha_id, nome, latitude, longitude, ordem, sentido) OVERRIDING SYSTEM VALUE VALUES (51, 3, 'Capela Santa Maria', -23.00154332, -49.88297498, 4, 'VOLTA');
INSERT INTO public.parada (id, linha_id, nome, latitude, longitude, ordem, sentido) OVERRIDING SYSTEM VALUE VALUES (52, 3, 'SuperMercado Avenida', -22.99333185, -49.87653480, 5, 'VOLTA');
INSERT INTO public.parada (id, linha_id, nome, latitude, longitude, ordem, sentido) OVERRIDING SYSTEM VALUE VALUES (53, 3, 'EE Maria do Carmo', -22.99099625, -49.87424288, 6, 'VOLTA');
INSERT INTO public.parada (id, linha_id, nome, latitude, longitude, ordem, sentido) OVERRIDING SYSTEM VALUE VALUES (54, 3, 'MicroTec', -22.98766091, -49.86810337, 7, 'VOLTA');
INSERT INTO public.parada (id, linha_id, nome, latitude, longitude, ordem, sentido) OVERRIDING SYSTEM VALUE VALUES (55, 3, 'Casa do Chopp', -22.98678054, -49.86592592, 8, 'VOLTA');
INSERT INTO public.parada (id, linha_id, nome, latitude, longitude, ordem, sentido) OVERRIDING SYSTEM VALUE VALUES (56, 3, 'Sebrae', -22.98149047, -49.86647449, 9, 'VOLTA');
INSERT INTO public.parada (id, linha_id, nome, latitude, longitude, ordem, sentido) OVERRIDING SYSTEM VALUE VALUES (57, 3, 'Terminal Centro', -22.97627412, -49.86949015, 10, 'VOLTA');

-- Linha 104 - Distrito Industrial (IDA)
INSERT INTO public.parada (id, linha_id, nome, latitude, longitude, ordem, sentido) OVERRIDING SYSTEM VALUE VALUES (58, 4, 'Terminal Centro', -22.97627412, -49.86949015, 1, 'IDA');
INSERT INTO public.parada (id, linha_id, nome, latitude, longitude, ordem, sentido) OVERRIDING SYSTEM VALUE VALUES (59, 4, 'Praça dos Ferroviários', -22.97806822, -49.87325683, 2, 'IDA');
INSERT INTO public.parada (id, linha_id, nome, latitude, longitude, ordem, sentido) OVERRIDING SYSTEM VALUE VALUES (60, 4, 'Bombeiro', -22.97845259, -49.87713480, 3, 'IDA');
INSERT INTO public.parada (id, linha_id, nome, latitude, longitude, ordem, sentido) OVERRIDING SYSTEM VALUE VALUES (61, 4, 'Etec Jácinto Ferreira de Sá', -22.97940994, -49.88023961, 4, 'IDA');
INSERT INTO public.parada (id, linha_id, nome, latitude, longitude, ordem, sentido) OVERRIDING SYSTEM VALUE VALUES (62, 4, 'Clinica Pontara', -22.97705752, -49.88337939, 5, 'IDA');
INSERT INTO public.parada (id, linha_id, nome, latitude, longitude, ordem, sentido) OVERRIDING SYSTEM VALUE VALUES (63, 4, 'Junior CarCare', -22.97624379, -49.88563625, 6, 'IDA');
INSERT INTO public.parada (id, linha_id, nome, latitude, longitude, ordem, sentido) OVERRIDING SYSTEM VALUE VALUES (64, 4, 'Lagoa Unimed', -22.97542771, -49.89238621, 7, 'IDA');
INSERT INTO public.parada (id, linha_id, nome, latitude, longitude, ordem, sentido) OVERRIDING SYSTEM VALUE VALUES (65, 4, 'Unimed', -22.97632846, -49.89348546, 8, 'IDA');
INSERT INTO public.parada (id, linha_id, nome, latitude, longitude, ordem, sentido) OVERRIDING SYSTEM VALUE VALUES (66, 4, 'Estácio Ourinhos', -22.97605501, -49.89634899, 9, 'IDA');
INSERT INTO public.parada (id, linha_id, nome, latitude, longitude, ordem, sentido) OVERRIDING SYSTEM VALUE VALUES (67, 4, 'Toyota', -22.97470479, -49.89866959, 10, 'IDA');
INSERT INTO public.parada (id, linha_id, nome, latitude, longitude, ordem, sentido) OVERRIDING SYSTEM VALUE VALUES (68, 4, 'CREA-SP', -22.97419243, -49.91020629, 11, 'IDA');
INSERT INTO public.parada (id, linha_id, nome, latitude, longitude, ordem, sentido) OVERRIDING SYSTEM VALUE VALUES (69, 4, 'Castor', -22.97491212, -49.91242031, 12, 'IDA');
INSERT INTO public.parada (id, linha_id, nome, latitude, longitude, ordem, sentido) OVERRIDING SYSTEM VALUE VALUES (70, 4, 'CWA Indústria Mecânica', -22.96857150, -49.91653318, 13, 'IDA');

-- Linha 104 - Distrito Industrial (VOLTA)
INSERT INTO public.parada (id, linha_id, nome, latitude, longitude, ordem, sentido) OVERRIDING SYSTEM VALUE VALUES (85, 4, 'CWA Indústria Mecânica', -22.96857150, -49.91653318, 1, 'VOLTA');
INSERT INTO public.parada (id, linha_id, nome, latitude, longitude, ordem, sentido) OVERRIDING SYSTEM VALUE VALUES (71, 4, 'Mestra Artesanal', -22.96890938, -49.91831167, 2, 'VOLTA');
INSERT INTO public.parada (id, linha_id, nome, latitude, longitude, ordem, sentido) OVERRIDING SYSTEM VALUE VALUES (72, 4, 'Café Jaguari', -22.97175619, -49.91489039, 3, 'VOLTA');
INSERT INTO public.parada (id, linha_id, nome, latitude, longitude, ordem, sentido) OVERRIDING SYSTEM VALUE VALUES (73, 4, 'Emei Itaipava', -22.97641005, -49.90778643, 4, 'VOLTA');
INSERT INTO public.parada (id, linha_id, nome, latitude, longitude, ordem, sentido) OVERRIDING SYSTEM VALUE VALUES (74, 4, 'CEEP', -22.97391048, -49.90616353, 5, 'VOLTA');
INSERT INTO public.parada (id, linha_id, nome, latitude, longitude, ordem, sentido) OVERRIDING SYSTEM VALUE VALUES (75, 4, 'Coletto 3R - Ourinhos', -22.97493978, -49.89792796, 6, 'VOLTA');
INSERT INTO public.parada (id, linha_id, nome, latitude, longitude, ordem, sentido) OVERRIDING SYSTEM VALUE VALUES (76, 4, 'Ponto Unimed', -22.97631123, -49.89430473, 7, 'VOLTA');
INSERT INTO public.parada (id, linha_id, nome, latitude, longitude, ordem, sentido) OVERRIDING SYSTEM VALUE VALUES (77, 4, 'Pão de Açucar', -22.97644625, -49.88565854, 8, 'VOLTA');
INSERT INTO public.parada (id, linha_id, nome, latitude, longitude, ordem, sentido) OVERRIDING SYSTEM VALUE VALUES (78, 4, 'Etec Jacinto Ferreiro de Sá', -22.97956688, -49.88036113, 9, 'VOLTA');
INSERT INTO public.parada (id, linha_id, nome, latitude, longitude, ordem, sentido) OVERRIDING SYSTEM VALUE VALUES (79, 4, 'LEC Idiomas', -22.97893388, -49.87800877, 10, 'VOLTA');
INSERT INTO public.parada (id, linha_id, nome, latitude, longitude, ordem, sentido) OVERRIDING SYSTEM VALUE VALUES (80, 4, 'Farmácias Nissei', -22.97803743, -49.87086702, 11, 'VOLTA');
INSERT INTO public.parada (id, linha_id, nome, latitude, longitude, ordem, sentido) OVERRIDING SYSTEM VALUE VALUES (81, 4, 'Terminal Centro', -22.97627412, -49.86949015, 12, 'VOLTA');

-- Atualiza sequences de identidade para evitar colisao em novas insercoes
SELECT pg_catalog.setval('public.linha_id_seq', COALESCE((SELECT MAX(id) FROM public.linha), 1), EXISTS (SELECT 1 FROM public.linha));
SELECT pg_catalog.setval('public.parada_id_seq', COALESCE((SELECT MAX(id) FROM public.parada), 1), EXISTS (SELECT 1 FROM public.parada));

COMMIT;
