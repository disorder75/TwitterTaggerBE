CREATE TABLE `TRAINING_DATASETS` (
    `Id` INT NOT NULL AUTO_INCREMENT,
    `Topic` TEXT NOT NULL,
    `Value` LONGTEXT NOT NULL,
    `CreationDate` DATETIME NULL,
    PRIMARY KEY (`Id`)
);