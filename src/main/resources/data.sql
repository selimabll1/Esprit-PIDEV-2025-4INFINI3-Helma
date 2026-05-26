-- Script d'initialisation des données pour les tests

-- Insertion de l'utilisateur courant pour les tests
INSERT INTO user (id, prenom, nom, created_at) VALUES 
(3, 'aziz', 'hachena', NOW());

-- Insérer d'autres utilisateurs si nécessaire pour les tests
INSERT INTO user (id, prenom, nom, created_at) VALUES 
(1, 'yassine', 'kamoun', NOW()),
(2, 'mahmoud', 'lajmi', NOW());
