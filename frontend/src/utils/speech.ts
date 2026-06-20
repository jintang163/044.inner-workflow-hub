const PUNCTUATION_KEYWORDS: Array<{ keyword: string; punctuation: string; removeKeyword: boolean }> = [
  { keyword: '句号', punctuation: '。', removeKeyword: true },
  { keyword: '逗号', punctuation: '，', removeKeyword: true },
  { keyword: '顿号', punctuation: '、', removeKeyword: true },
  { keyword: '分号', punctuation: '；', removeKeyword: true },
  { keyword: '冒号', punctuation: '：', removeKeyword: true },
  { keyword: '问号', punctuation: '？', removeKeyword: true },
  { keyword: '感叹号', punctuation: '！', removeKeyword: true },
  { keyword: '引号', punctuation: '"', removeKeyword: true },
  { keyword: '左引号', punctuation: '"', removeKeyword: true },
  { keyword: '右引号', punctuation: '"', removeKeyword: true },
  { keyword: '括号', punctuation: '（）', removeKeyword: true },
  { keyword: '左括号', punctuation: '（', removeKeyword: true },
  { keyword: '右括号', punctuation: '）', removeKeyword: true },
  { keyword: '省略号', punctuation: '……', removeKeyword: true },
  { keyword: '破折号', punctuation: '——', removeKeyword: true },
  { keyword: '换行', punctuation: '\n', removeKeyword: true },
  { keyword: '回车', punctuation: '\n', removeKeyword: true }
]

const SENTENCE_END_KEYWORDS = ['吗', '呢', '吧', '啊', '呀', '嘛']

const QUESTION_PATTERNS = [
  /是不是$/,
  /对不对$/,
  /好不好$/,
  /行不行$/,
  /能不能$/,
  /有没有$/,
  /可以吗$/,
  /行吗$/,
  /是吗$/,
  /对吗$/,
  /好吗$/
]

export const addPunctuation = (text: string): string => {
  if (!text) return ''

  let result = text

  PUNCTUATION_KEYWORDS.forEach(({ keyword, punctuation, removeKeyword }) => {
    if (removeKeyword) {
      result = result.replace(new RegExp(keyword, 'g'), punctuation)
    }
  })

  const sentences = result.split(/(?<=[。！？])/).filter(Boolean)

  const processedSentences = sentences.map((sentence) => {
    let s = sentence.trim()

    if (/[。，、；：？！""（）……——\n]$/.test(s)) {
      return s
    }

    for (const pattern of QUESTION_PATTERNS) {
      if (pattern.test(s)) {
        return s + '？'
      }
    }

    const lastChar = s.slice(-1)
    if (SENTENCE_END_KEYWORDS.includes(lastChar) && s.length > 2) {
      return s + '。'
    }

    if (s.length >= 4 && /[的了]$/.test(lastChar)) {
      return s + '。'
    }

    return s
  })

  result = processedSentences.join('')

  if (result && !/[。！？\n]$/.test(result)) {
    result += '。'
  }

  return result
}

export const cleanTranscript = (text: string): string => {
  if (!text) return ''

  let result = text.trim()

  result = result.replace(/\s+/g, '')

  result = result.replace(/[.]{2,}/g, '……')
  result = result.replace(/[,]{2,}/g, '，')

  return result
}
