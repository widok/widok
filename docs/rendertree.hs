#!/usr/bin/env runhaskell

-- Written by Pepijn Kokke
-- See https://gist.github.com/pepijnkokke/61cf8a970b99f0c92d4b

-- cabal depedencies:
--
--   * containers
--   * pandoc-types
--   * uu-parsinglib
--

{-# LANGUAGE RankNTypes #-}
{-# LANGUAGE FlexibleContexts #-}

import           Control.Applicative
import           Data.Char (isSpace)
import qualified Data.Map as M (singleton)
import           Data.Monoid
import           Data.Tree (Tree(..))
import           Debug.Trace (trace)
import           Text.Pandoc.JSON
import           Text.Pandoc.Walk (walk)
import           Text.ParserCombinators.UU
import           Text.ParserCombinators.UU.BasicInstances
import           Text.ParserCombinators.UU.Derived
import           Text.ParserCombinators.UU.Utils


-- Syntax of trees is as follows:
--
--  Node         ::= '[' Value (Node | Leaf)*  ']'
--  Leaf         ::= QuotedString | MathString | LabelString
--  QuotedString ::= '"' ... anything goes ... '"'
--  MathString   ::= '$' ... anything goes ... '$'
--  LabelString  ::=  anything but a space ... ' '
--


-- * Parsing Tree format and outputting LaTeX's QTree format

renderTree' :: Block -> Block
renderTree' cb@(CodeBlock (_, classes, _) str)
  | "tree" `elem` classes = RawBlock (Format "latex") (showQTree . parseTree $ str)
  | otherwise             = cb
renderTree' bl            = bl

parseTree :: String -> Tree String
parseTree = runParser "tree" pNode
  where
    pNode :: Parser (Tree String)
    pNode = pBrackets (Node <$> pValue <*> pMany pNode) <<|> (return <$> pValue)
    pValue,pLabelString,pMathString :: Parser String
    pValue       = pQuotedString <<|> pMathString <<|> pLabelString
    pMathString  = (\str -> "$" ++ str ++ "$") <$> pParentheticalString '$'
    pLabelString = lexeme $ pList (pPred (\c -> not (isSpace c || c == ']'))) <* (pAnySym " \r\n\t" <?> "Whitespace")
    pPred p      = pSatisfy p
      (Insertion  "Anything but whitespace" 'y' 5)



showQTree :: Tree String -> String
showQTree t = unlines
  [ "\\begin{tikzpicture}"
  , "\\Tree " ++ go t
  , "\\end{tikzpicture}"
  ]
  where
    go (Node val []) = leaf val
    go (Node lbl xs) = node lbl . unwords . map go $ xs
    leaf val   = "{" ++ val ++ "}"
    node  "" x = "[ "                 ++ x ++ " ]"
    node lbl x = "[.{" ++ lbl ++ "} " ++ x ++ " ]"


-- * Adding LaTeX includes

unsafeUse :: String -> String
unsafeUse pkg =
  "\\usepackage{" ++ pkg ++ "}%"

unsafeQTreeUse :: Block
unsafeQTreeUse =
  RawBlock (Format "latex") . unlines $
  [ unsafeUse "tikz"
  , unsafeUse "tikz-qtree"
  ]

useQTree :: Meta -> Meta
useQTree meta = case lookupMeta "header-uses" meta of
  Nothing                  -> mkMeta [unsafeQTreeUse]
  Just (MetaBlocks blocks) -> mkMeta (unsafeQTreeUse : blocks)
  Just _                   -> trace "warning: tikz-qtree was not included" meta
  where
    mkMeta :: [Block] -> Meta
    mkMeta = (<> meta) . Meta . M.singleton "header-includes" . MetaBlocks


-- * Filter definition

renderTree :: Pandoc -> Pandoc
renderTree (Pandoc meta blocks) = Pandoc (useQTree meta) (walk renderTree' blocks)

main :: IO ()
main = toJSONFilter renderTree
